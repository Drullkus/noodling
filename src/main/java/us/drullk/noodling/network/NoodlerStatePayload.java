package us.drullk.noodling.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import us.drullk.noodling.Noodling;
import us.drullk.noodling.noodler.NoodlerState;

/**
 * Client to server: the whole GUI state of the main hand Noodler after an edit was committed.
 *
 * <p>Written by Claude Code; conversation transcript: {@code llm/transcripts/transcript_2026-10-10_25b84f50-3f88-41a1-a942-98824ffe67dc.md}.
 */
public record NoodlerStatePayload(NoodlerState state) implements CustomPacketPayload {

    public static final Type<NoodlerStatePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Noodling.MODID, "noodler_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, NoodlerStatePayload> STREAM_CODEC =
            StreamCodec.composite(NoodlerState.STREAM_CODEC, NoodlerStatePayload::state, NoodlerStatePayload::new);

    @Override
    public Type<NoodlerStatePayload> type() {
        return TYPE;
    }
}
