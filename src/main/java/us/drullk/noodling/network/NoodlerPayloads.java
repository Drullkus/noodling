package us.drullk.noodling.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import us.drullk.noodling.noodler.NoodlerCharge;
import us.drullk.noodling.noodler.NoodlerComponents;
import us.drullk.noodling.noodler.NoodlerItem;
import us.drullk.noodling.noodler.NoodlerPlacement;

/**
 * Registers the Noodler payloads and handles them on the server. Both handlers only act on a Noodler in the
 * sender's main hand, so a stale or forged packet cannot edit another item.
 *
 * <p>Written by Claude Code; conversation transcript: {@code llm/transcripts/transcript_2026-10-10_25b84f50-3f88-41a1-a942-98824ffe67dc.md}.
 */
public final class NoodlerPayloads {

    /** Latency allowance: the server may have seen the charge start a little after the client did. */
    private static final int RELEASE_TOLERANCE_TICKS = 2;

    private NoodlerPayloads() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(NoodlerStatePayload.TYPE, NoodlerStatePayload.STREAM_CODEC, NoodlerPayloads::handleState);
        registrar.playToServer(NoodlerChargePayload.TYPE, NoodlerChargePayload.STREAM_CODEC, NoodlerPayloads::handleCharge);
    }

    private static void handleState(NoodlerStatePayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        if (stack.getItem() instanceof NoodlerItem) {
            stack.set(NoodlerComponents.STATE.get(), payload.state());
        }
    }

    private static void handleCharge(NoodlerChargePayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        boolean holdsNoodler = stack.getItem() instanceof NoodlerItem;
        switch (payload.action()) {
            case START -> {
                NoodlerPlacement.clearCharges(player);
                if (holdsNoodler) {
                    stack.set(NoodlerComponents.CHARGE.get(), new NoodlerCharge(player.level().getGameTime()));
                }
            }
            case RELEASE -> {
                NoodlerCharge charge = NoodlerPlacement.mainHandCharge(player);
                NoodlerPlacement.clearCharges(player);
                if (holdsNoodler && charge != null && charge.ticksHeld(player.level().getGameTime()) >= NoodlerPlacement.CHARGE_TICKS - RELEASE_TOLERANCE_TICKS) {
                    NoodlerPlacement.place(player, stack);
                }
            }
            case CANCEL -> NoodlerPlacement.clearCharges(player);
        }
    }
}
