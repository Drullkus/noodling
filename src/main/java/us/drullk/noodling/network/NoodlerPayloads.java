package us.drullk.noodling.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import us.drullk.noodling.Noodling;
import us.drullk.noodling.noodler.NoodlerCharge;
import us.drullk.noodling.noodler.NoodlerComponents;
import us.drullk.noodling.noodler.NoodlerExport;
import us.drullk.noodling.noodler.NoodlerItem;
import us.drullk.noodling.noodler.NoodlerPlacement;

import java.util.function.Consumer;

/**
 * Registers the Noodler payloads and handles the server-bound ones. Every server handler only acts on a Noodler in
 * the sender's main hand, so a stale or forged packet cannot edit another item. Export results go to
 * {@link #exportResultSink}, which the client entry point points at its file writer; on a dedicated server it stays
 * a no-op.
 *
 * <p>Written by Claude Code; conversation transcript: {@code llm/transcripts/transcript_2026-10-10_25b84f50-3f88-41a1-a942-98824ffe67dc.md}.
 */
public final class NoodlerPayloads {

    /** Latency allowance: the server may have seen the charge start a little after the client did. */
    private static final int RELEASE_TOLERANCE_TICKS = 2;

    /** Receives export replies on the client; set by the client entry point. */
    public static Consumer<NoodlerExportResultPayload> exportResultSink = payload -> {
    };

    private NoodlerPayloads() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(NoodlerStatePayload.TYPE, NoodlerStatePayload.STREAM_CODEC, NoodlerPayloads::handleState);
        registrar.playToServer(NoodlerChargePayload.TYPE, NoodlerChargePayload.STREAM_CODEC, NoodlerPayloads::handleCharge);
        registrar.playToServer(NoodlerExportRequestPayload.TYPE, NoodlerExportRequestPayload.STREAM_CODEC, NoodlerPayloads::handleExportRequest);
        registrar.playToClient(NoodlerExportResultPayload.TYPE, NoodlerExportResultPayload.STREAM_CODEC, (payload, context) -> exportResultSink.accept(payload));
    }

    private static void handleExportRequest(NoodlerExportRequestPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        NoodlerExportResultPayload result;
        if (!(stack.getItem() instanceof NoodlerItem)) {
            result = NoodlerExportResultPayload.failure(payload.requestId(), "noodling.export.no_noodler");
        } else {
            try {
                byte[] bytes = NoodlerExport.compress(NoodlerExport.buildTemplate(player, stack));
                result = bytes.length > NoodlerExport.MAX_BYTES
                        ? NoodlerExportResultPayload.failure(payload.requestId(), "noodling.export.too_large")
                        : NoodlerExportResultPayload.success(payload.requestId(), bytes);
            } catch (Exception e) {
                Noodling.LOGGER.error("Noodler export failed for {}", player.getScoreboardName(), e);
                result = NoodlerExportResultPayload.failure(payload.requestId(), "noodling.export.failed");
            }
        }
        PacketDistributor.sendToPlayer(player, result);
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
