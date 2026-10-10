package us.drullk.noodling.client.imgui;

import imgui.ImDrawData;
import imgui.ImFontConfig;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.flag.ImGuiBackendFlags;
import imgui.internal.ImGuiContext;
import org.jspecify.annotations.Nullable;

/**
 * Owns the Dear ImGui context for the mod.
 *
 * <p>The context is created on first use and rebuilt whenever Minecraft's GUI scale changes: the default font is
 * rasterised at {@value #BASE_FONT_SIZE} pixels times the GUI scale and every style size is multiplied by the same
 * factor, so the panel follows the GUI Scale setting while ImGui itself works in physical pixels. No .ini file is
 * written. A frame runs only while a screen asks for one.
 *
 * <p>Written by Claude Code; conversation transcript: {@code llm/transcripts/transcript_2026-10-10_25b84f50-3f88-41a1-a942-98824ffe67dc.md}.
 */
public final class NoodlingImGui {

    /** ImGui's default font size, in pixels, before GUI scaling. */
    public static final float BASE_FONT_SIZE = 13.0F;

    private static @Nullable ImGuiContext context;
    private static int contextScale = -1;
    private static long lastFrameNanos;

    private NoodlingImGui() {
    }

    /** Creates the context for {@code guiScale}, replacing one built for a different scale. */
    public static void ensureContext(int guiScale) {
        int scale = Math.max(1, guiScale);
        if (context != null && contextScale == scale) {
            return;
        }
        destroy();

        context = ImGui.createContext();
        contextScale = scale;
        ImGuiIO io = ImGui.getIO();
        io.setIniFilename(null);
        io.setBackendPlatformName("noodling_minecraft");
        io.setBackendRendererName("noodling_blaze3d");
        io.addBackendFlags(ImGuiBackendFlags.RendererHasVtxOffset);

        ImFontConfig fontConfig = new ImFontConfig();
        fontConfig.setSizePixels(BASE_FONT_SIZE * scale);
        io.getFonts().addFontDefault(fontConfig);
        fontConfig.destroy();

        ImGui.styleColorsDark();
        ImGui.getStyle().scaleAllSizes(scale);

        ImGuiRenderer.uploadFontAtlas(io.getFonts());
        lastFrameNanos = 0L;
    }

    public static boolean hasContext() {
        return context != null;
    }

    /** Frees the context and its GPU resources. */
    public static void destroy() {
        if (context != null) {
            ImGuiRenderer.releaseFontAtlas();
            ImGui.destroyContext(context);
            context = null;
            contextScale = -1;
        }
    }

    /** The GUI scale the current context was built for. */
    public static int scale() {
        return Math.max(1, contextScale);
    }

    /** Converts a size in GUI-scaled units to physical pixels for the current context. */
    public static float px(float guiUnits) {
        return guiUnits * scale();
    }

    /** Starts a frame that covers the whole framebuffer. */
    public static void beginFrame(int framebufferWidth, int framebufferHeight) {
        ImGuiIO io = ImGui.getIO();
        io.setDisplaySize(Math.max(1, framebufferWidth), Math.max(1, framebufferHeight));
        io.setDisplayFramebufferScale(1.0F, 1.0F);

        long now = System.nanoTime();
        float delta = lastFrameNanos == 0L ? 1.0F / 60.0F : (now - lastFrameNanos) / 1.0e9F;
        lastFrameNanos = now;
        io.setDeltaTime(Math.max(delta, 1.0e-4F));

        ImGui.newFrame();
    }

    /** Ends the frame and returns the draw data for {@link ImGuiRenderer#submit}. */
    public static ImDrawData endFrame() {
        ImGui.render();
        return ImGui.getDrawData();
    }
}
