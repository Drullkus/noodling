package us.drullk.noodling.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import us.drullk.noodling.Noodling;

/**
 * Client to server: build the structure template of the main hand Noodler. The id is echoed in the reply so the
 * client can match it to the file the player chose.
 *
 * <p>Written by Claude Code; conversation transcript: {@code llm/transcripts/transcript_2026-10-10_25b84f50-3f88-41a1-a942-98824ffe67dc.md}.
 */
public record NoodlerExportRequestPayload(int requestId) implements CustomPacketPayload {

    public static final Type<NoodlerExportRequestPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Noodling.MODID, "noodler_export_request"));
    public static final StreamCodec<ByteBuf, NoodlerExportRequestPayload> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(NoodlerExportRequestPayload::new, NoodlerExportRequestPayload::requestId);

    @Override
    public Type<NoodlerExportRequestPayload> type() {
        return TYPE;
    }
}
