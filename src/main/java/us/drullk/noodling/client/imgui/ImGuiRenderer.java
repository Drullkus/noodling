package us.drullk.noodling.client.imgui;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.buffers.TransientMemory;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.renderpearl.api.pipeline.IndexType;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import imgui.ImDrawData;
import imgui.ImFontAtlas;
import imgui.ImVec4;
import imgui.type.ImInt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;
import us.drullk.noodling.Noodling;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Draws Dear ImGui draw data through Minecraft's own GPU abstraction, so it works on both the Vulkan and the OpenGL
 * backend of Minecraft 26.3 without touching GL state.
 *
 * <p>ImGui vertices (2D position, UV, packed RGBA) are rewritten into the vanilla {@code POSITION_TEX_COLOR} layout
 * each frame into transient GPU memory and drawn with a copy of the GUI textured pipeline that uses triangles and
 * no depth test. The font atlas is the only texture. The draw happens in {@link RenderFrameEvent.Post}, after the
 * whole game frame and before the swap-chain blit, so the panel sits on top of every vanilla GUI layer.
 *
 * <p>Written by Claude Code; conversation transcript: {@code llm/transcripts/transcript_2026-10-10_25b84f50-3f88-41a1-a942-98824ffe67dc.md}.
 */
public final class ImGuiRenderer {

    /** The only texture id ImGui sees; it always means the font atlas. */
    private static final long FONT_TEXTURE_ID = 1L;
    /** Bytes per ImGui vertex: two floats of position, two floats of UV, one packed colour. */
    private static final int IMGUI_VERTEX_STRIDE = 20;
    private static final int VERTEX_STRIDE = DefaultVertexFormat.POSITION_TEX_COLOR.getVertexSize();
    /** Same depth range the vanilla GUI renderer uses, so the ortho projection is set up identically. */
    private static final float Z_NEAR = 1000.0F;
    private static final float Z_FAR = 11000.0F;

    public static final RenderPipeline PIPELINE = RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(Noodling.MODID, "pipeline/imgui"))
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withDepthStencilState(Optional.empty())
            .withCull(false)
            .build();

    private static final Projection PROJECTION = new Projection();
    private static @Nullable ProjectionMatrixBuffer projectionBuffer;
    private static @Nullable GpuTexture fontTexture;
    private static @Nullable GpuTextureView fontView;
    private static @Nullable ImDrawData pending;

    private ImGuiRenderer() {
    }

    public static void registerPipeline(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(PIPELINE);
    }

    /** Queues draw data produced during extraction to be drawn at the end of this frame. */
    public static void submit(ImDrawData drawData) {
        pending = drawData;
    }

    public static void onRenderFrame(RenderFrameEvent.Post event) {
        ImDrawData drawData = pending;
        pending = null;
        if (drawData != null && fontView != null) {
            render(drawData);
        }
    }

    /** Uploads the atlas of {@code fonts} as the font texture and tags the atlas with {@link #FONT_TEXTURE_ID}. */
    static void uploadFontAtlas(ImFontAtlas fonts) {
        releaseFontAtlas();
        ImInt width = new ImInt();
        ImInt height = new ImInt();
        ByteBuffer pixels = fonts.getTexDataAsRGBA32(width, height);
        int w = width.get();
        int h = height.get();

        GpuDevice device = RenderSystem.getDevice();
        GpuTexture texture = device.createTexture("Noodling ImGui font atlas", GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_COPY_DST, GpuFormat.RGBA8_UNORM, w, h, 1, 1);
        if (pixels.isDirect()) {
            device.createCommandEncoder().writeToTexture(texture, pixels, 0, 0, 0, 0, w, h);
        } else {
            ByteBuffer direct = MemoryUtil.memAlloc(pixels.remaining());
            try {
                direct.put(pixels.duplicate()).flip();
                device.createCommandEncoder().writeToTexture(texture, direct, 0, 0, 0, 0, w, h);
            } finally {
                MemoryUtil.memFree(direct);
            }
        }
        fontTexture = texture;
        fontView = device.createTextureView(texture);
        fonts.setTexID(FONT_TEXTURE_ID);
    }

    static void releaseFontAtlas() {
        if (fontView != null) {
            fontView.close();
            fontView = null;
        }
        if (fontTexture != null) {
            fontTexture.close();
            fontTexture = null;
        }
    }

    private static void render(ImDrawData drawData) {
        RenderTarget target = Minecraft.getInstance().gameRenderer.mainRenderTarget();
        GpuTextureView colorView = target.getColorTextureView();
        GpuTextureView font = fontView;
        if (colorView == null || font == null) {
            return;
        }
        float displayWidth = drawData.getDisplaySizeX();
        float displayHeight = drawData.getDisplaySizeY();
        if (!(displayWidth > 0.0F) || !(displayHeight > 0.0F)) {
            return;
        }
        int framebufferWidth = colorView.getWidth(0);
        int framebufferHeight = colorView.getHeight(0);
        float scaleX = framebufferWidth / displayWidth;
        float scaleY = framebufferHeight / displayHeight;

        if (projectionBuffer == null) {
            projectionBuffer = new ProjectionMatrixBuffer("noodling_imgui");
        }
        PROJECTION.setupOrtho(Z_NEAR, Z_FAR, displayWidth, displayHeight, true);
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(projectionBuffer.getBuffer(PROJECTION), ProjectionType.ORTHOGRAPHIC);
        try {
            GpuBufferSlice transforms = RenderSystem.getDynamicUniforms().writeTransform(new Matrix4f().setTranslation(0.0F, 0.0F, -Z_FAR));
            GpuSampler sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
            CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
            TransientMemory transientMemory = encoder.transientMemory();

            try (RenderPass pass = encoder.createRenderPass(() -> "Noodling ImGui", colorView, Optional.empty(),
                    target.hasDepth() ? target.getDepthTextureView() : null, OptionalDouble.empty())) {
                RenderSystem.bindDefaultUniforms(pass);
                pass.setUniform("DynamicTransforms", transforms);
                pass.setPipeline(RenderSystem.getCompiledPipeline(PIPELINE));
                pass.setUniform("Sampler0", font, sampler);

                ImVec4 clip = new ImVec4();
                for (int list = 0; list < drawData.getCmdListsCount(); list++) {
                    int vertexCount = drawData.getCmdListVtxBufferSize(list);
                    int indexCount = drawData.getCmdListIdxBufferSize(list);
                    if (vertexCount == 0 || indexCount == 0) {
                        continue;
                    }
                    GpuBufferSlice vertices = convertVertices(transientMemory, drawData.getCmdListVtxBufferData(list), vertexCount);
                    GpuBufferSlice indices = transientMemory.uploadGpu(drawData.getCmdListIdxBufferData(list), 4L, GpuBuffer.USAGE_INDEX);
                    pass.setVertexBuffer(0, vertices);
                    pass.setIndexBuffer(indices.buffer(), IndexType.SHORT);
                    int baseIndex = (int) (indices.offset() / IndexType.SHORT.bytes);

                    for (int cmd = 0; cmd < drawData.getCmdListCmdBufferSize(list); cmd++) {
                        int elements = drawData.getCmdListCmdBufferElemCount(list, cmd);
                        if (elements == 0) {
                            continue;
                        }
                        drawData.getCmdListCmdBufferClipRect(clip, list, cmd);
                        int left = clamp(clip.x * scaleX, framebufferWidth);
                        int top = clamp(clip.y * scaleY, framebufferHeight);
                        int right = clamp(clip.z * scaleX, framebufferWidth);
                        int bottom = clamp(clip.w * scaleY, framebufferHeight);
                        if (right <= left || bottom <= top) {
                            continue;
                        }
                        pass.enableScissor(left, framebufferHeight - bottom, right - left, bottom - top);
                        pass.drawIndexed(elements, 1, baseIndex + drawData.getCmdListCmdBufferIdxOffset(list, cmd), drawData.getCmdListCmdBufferVtxOffset(list, cmd), 0);
                    }
                }
            }
        } finally {
            RenderSystem.restoreProjectionMatrix();
        }
    }

    /** Rewrites ImGui vertices into {@code POSITION_TEX_COLOR} in transient CPU memory and uploads them. */
    private static GpuBufferSlice convertVertices(TransientMemory transientMemory, ByteBuffer source, int vertexCount) {
        ByteBuffer src = source.order(ByteOrder.nativeOrder());
        long size = (long) vertexCount * VERTEX_STRIDE;
        ByteBuffer dst = transientMemory.allocateCpu(size, 16L).order(ByteOrder.nativeOrder());
        for (int v = 0; v < vertexCount; v++) {
            int in = v * IMGUI_VERTEX_STRIDE;
            int out = v * VERTEX_STRIDE;
            dst.putFloat(out, src.getFloat(in));
            dst.putFloat(out + 4, src.getFloat(in + 4));
            dst.putFloat(out + 8, 0.0F);
            dst.putFloat(out + 12, src.getFloat(in + 8));
            dst.putFloat(out + 16, src.getFloat(in + 12));
            dst.putInt(out + 20, src.getInt(in + 16));
        }
        dst.position(0).limit((int) size);
        return transientMemory.uploadGpu(dst, 16L, GpuBuffer.USAGE_VERTEX);
    }

    private static int clamp(float value, int max) {
        return (int) Math.max(0.0F, Math.min(max, value));
    }
}
