package us.drullk.noodling.client.noodler;

import com.mojang.blaze3d.platform.Window;
import imgui.ImGuiIO;
import imgui.ImGui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import tamaized.beanification.Autowired;
import us.drullk.noodling.Noodling;
import us.drullk.noodling.client.imgui.ImGuiInput;
import us.drullk.noodling.client.imgui.ImGuiRenderer;
import us.drullk.noodling.client.imgui.NoodlingImGui;
import us.drullk.noodling.network.NoodlerStatePayload;
import us.drullk.noodling.noodler.*;

/**
 * The Noodler settings screen. Vanilla provides the freed cursor and the darkened, blurred background; everything
 * inside is Dear ImGui.
 *
 * <p>Each frame the screen runs one ImGui frame during GUI extraction, hands the draw data to
 * {@link ImGuiRenderer}, and draws the rotated inner-space axes as gizmo arrows at the live crosshair hit. Mouse and
 * keyboard events are forwarded to ImGui; only Escape is kept, to close the screen.
 *
 * <p>Written by Claude Code; conversation transcript: {@code llm/transcripts/transcript_2026-10-10_25b84f50-3f88-41a1-a942-98824ffe67dc.md}.
 */
public final class NoodlerScreen extends Screen {

    @Autowired(dist = Dist.CLIENT)
    private static PastaShapes pastaShapes;

    /** Length of each axis arrow, in blocks. */
    private static final double AXIS_LENGTH = 8.0;
    private static final int AXIS_X_COLOR = 0xFFFF4040;
    private static final int AXIS_Y_COLOR = 0xFF40FF40;
    private static final int AXIS_Z_COLOR = 0xFF4080FF;

    private final InteractionHand hand;
    private final NoodlerPanel panel;
    private boolean gizmoWarningLogged;

    public NoodlerScreen(InteractionHand hand) {
        super(Component.translatable("noodling.gui.title"));
        this.hand = hand;
        this.panel = new NoodlerPanel(this, pastaShapes);
        this.panel.load(this.currentState());
    }

    private ItemStack stack() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player == null ? ItemStack.EMPTY : player.getItemInHand(this.hand);
    }

    /** The state currently stored on the held Noodler. */
    public NoodlerState currentState() {
        return this.stack().getOrDefault(NoodlerComponents.STATE.get(), NoodlerState.DEFAULT);
    }

    /** Stores an edited state on the local stack and sends it to the server. */
    void commit(NoodlerState state) {
        ItemStack stack = this.stack();
        if (stack.getItem() instanceof NoodlerItem) {
            stack.set(NoodlerComponents.STATE.get(), state);
            ClientPacketDistributor.sendToServer(new NoodlerStatePayload(state));
        }
    }

    @Override
    protected void init() {
        NoodlingImGui.ensureContext(Minecraft.getInstance().getWindow().getGuiScale());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        Window window = Minecraft.getInstance().getWindow();
        NoodlingImGui.ensureContext(window.getGuiScale());
        NoodlingImGui.beginFrame(window.getWidth(), window.getHeight());
        try {
            this.panel.draw();
        } finally {
            ImGuiRenderer.submit(NoodlingImGui.endFrame());
        }
        this.extractAxes(partialTick);
    }

    private void extractAxes(float partialTick) {
        LocalPlayer player = Minecraft.getInstance().player;
        ParamValue.Rotation rotation = this.panel.currentRotation();
        if (player == null || rotation == null) {
            return;
        }
        Vec3 center = NoodlerRaycast.raycast(player, partialTick).getLocation();
        Quaterniond quaternion = rotation.toQuaternion();
        try {
            axis(center, quaternion, 1, 0, 0, AXIS_X_COLOR);
            axis(center, quaternion, 0, 1, 0, AXIS_Y_COLOR);
            axis(center, quaternion, 0, 0, 1, AXIS_Z_COLOR);
        } catch (IllegalStateException e) {
            if (!this.gizmoWarningLogged) {
                this.gizmoWarningLogged = true;
                Noodling.LOGGER.warn("Cannot draw Noodler axes: no gizmo collector is active during GUI extraction", e);
            }
        }
    }

    private static void axis(Vec3 center, Quaterniond rotation, double x, double y, double z, int argb) {
        Vector3d direction = rotation.transform(new Vector3d(x, y, z)).mul(AXIS_LENGTH);
        Gizmos.arrow(center, center.add(direction.x, direction.y, direction.z), argb).setAlwaysOnTop();
    }

    private static float px(double guiUnits) {
        return (float) (guiUnits * Minecraft.getInstance().getWindow().getGuiScale());
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (NoodlingImGui.hasContext()) {
            ImGui.getIO().addMousePosEvent(px(mouseX), px(mouseY));
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (!NoodlingImGui.hasContext()) {
            return false;
        }
        ImGuiIO io = ImGui.getIO();
        io.addMousePosEvent(px(event.x()), px(event.y()));
        ImGuiInput.modifiers(io, event.modifiers());
        int button = ImGuiInput.mouseButton(event.button());
        if (button >= 0) {
            io.addMouseButtonEvent(button, true);
        }
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (!NoodlingImGui.hasContext()) {
            return false;
        }
        ImGuiIO io = ImGui.getIO();
        io.addMousePosEvent(px(event.x()), px(event.y()));
        int button = ImGuiInput.mouseButton(event.button());
        if (button >= 0) {
            io.addMouseButtonEvent(button, false);
        }
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (NoodlingImGui.hasContext()) {
            ImGui.getIO().addMousePosEvent(px(event.x()), px(event.y()));
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (NoodlingImGui.hasContext()) {
            ImGui.getIO().addMouseWheelEvent((float) scrollX, (float) scrollY);
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.isEscape()) {
            return super.keyPressed(event);
        }
        if (NoodlingImGui.hasContext()) {
            ImGuiInput.keyEvent(ImGui.getIO(), event, true);
        }
        return true;
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        if (NoodlingImGui.hasContext()) {
            ImGuiInput.keyEvent(ImGui.getIO(), event, false);
        }
        return true;
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (NoodlingImGui.hasContext()) {
            ImGui.getIO().addInputCharacter(event.codepoint());
        }
        return true;
    }
}
