package us.drullk.noodling.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import us.drullk.noodling.Noodling;

/**
 * Client to server: left click was pressed, released after a full charge, or released early.
 *
 * <p>Written by Claude Code; conversation transcript: {@code llm/transcripts/transcript_2026-10-10_25b84f50-3f88-41a1-a942-98824ffe67dc.md}.
 */
public record NoodlerChargePayload(Action action) implements CustomPacketPayload {

    public enum Action {
        START,
        RELEASE,
        CANCEL;

        private static final Action[] VALUES = values();
    }

    public static final Type<NoodlerChargePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Noodling.MODID, "noodler_charge"));
    public static final StreamCodec<ByteBuf, NoodlerChargePayload> STREAM_CODEC = ByteBufCodecs.VAR_INT
            .map(ordinal -> Action.VALUES[Math.floorMod(ordinal, Action.VALUES.length)], Action::ordinal)
            .map(NoodlerChargePayload::new, NoodlerChargePayload::action);

    @Override
    public Type<NoodlerChargePayload> type() {
        return TYPE;
    }
}
