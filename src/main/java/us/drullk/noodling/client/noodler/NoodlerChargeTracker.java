package us.drullk.noodling.client.noodler;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import us.drullk.noodling.network.NoodlerChargePayload;
import us.drullk.noodling.noodler.NoodlerCharge;
import us.drullk.noodling.noodler.NoodlerComponents;
import us.drullk.noodling.noodler.NoodlerItem;
import us.drullk.noodling.noodler.NoodlerPlacement;

/**
 * Watches the attack key while a Noodler is in the main hand and no screen is open.
 *
 * <p>Pressing starts a charge: the charge marker is set on the local stack right away (so the pose shows without
 * waiting for the server) and the server is told. Releasing after {@link NoodlerPlacement#CHARGE_TICKS} ticks asks
 * the server to place; releasing earlier, opening a screen or switching items cancels. Vanilla attacking and block
 * breaking are cancelled while a Noodler is held, and the FOV narrows like a drawn bow.
 *
 * <p>Written by Claude Code; conversation transcript: {@code llm/transcripts/transcript_2026-10-10_25b84f50-3f88-41a1-a942-98824ffe67dc.md}.
 */
public final class NoodlerChargeTracker {

    private static boolean charging;
    private static long startTick;

    private NoodlerChargeTracker() {
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null) {
            charging = false;
            return;
        }
        ItemStack stack = player.getMainHandItem();
        boolean canCharge = minecraft.gui.screen() == null && stack.getItem() instanceof NoodlerItem && !player.isSpectator();
        boolean attackDown = minecraft.options.keyAttack.isDown();

        if (!charging) {
            if (canCharge && attackDown) {
                charging = true;
                startTick = level.getGameTime();
                stack.set(NoodlerComponents.CHARGE.get(), new NoodlerCharge(startTick));
                ClientPacketDistributor.sendToServer(new NoodlerChargePayload(NoodlerChargePayload.Action.START));
            }
        } else if (!canCharge || !attackDown) {
            charging = false;
            long held = level.getGameTime() - startTick;
            if (stack.getItem() instanceof NoodlerItem) {
                stack.remove(NoodlerComponents.CHARGE.get());
            }
            boolean release = canCharge && held >= NoodlerPlacement.CHARGE_TICKS;
            ClientPacketDistributor.sendToServer(new NoodlerChargePayload(release ? NoodlerChargePayload.Action.RELEASE : NoodlerChargePayload.Action.CANCEL));
        }
    }

    /** Left click with a Noodler never attacks or mines. */
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (event.isAttack() && player != null && player.getMainHandItem().getItem() instanceof NoodlerItem) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }

    /** Same zoom curve as a drawn bow. */
    public static void onComputeFov(ComputeFovModifierEvent event) {
        ItemStack stack = event.getPlayer().getMainHandItem();
        NoodlerCharge charge = stack.getItem() instanceof NoodlerItem ? stack.get(NoodlerComponents.CHARGE.get()) : null;
        if (charge == null) {
            return;
        }
        float draw = Math.min(charge.ticksHeld(event.getPlayer().level().getGameTime()) / 20.0F, 1.0F);
        event.setNewFovModifier(event.getNewFovModifier() * (1.0F - Mth.square(draw) * 0.15F));
    }
}
