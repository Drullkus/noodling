package us.drullk.noodling.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import us.drullk.noodling.Noodling;
import us.drullk.noodling.noodler.NoodlerExport;

/**
 * Server to client: the compressed structure template bytes for an export request, or an error key (a translation
 * key) with empty data.
 *
 * <p>Written by Claude Code; conversation transcript: {@code llm/transcripts/transcript_2026-10-10_25b84f50-3f88-41a1-a942-98824ffe67dc.md}.
 */
public record NoodlerExportResultPayload(int requestId, byte[] data, String errorKey) implements CustomPacketPayload {

    public static final Type<NoodlerExportResultPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Noodling.MODID, "noodler_export_result"));
    public static final StreamCodec<ByteBuf, NoodlerExportResultPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, NoodlerExportResultPayload::requestId,
            ByteBufCodecs.byteArray(NoodlerExport.MAX_BYTES), NoodlerExportResultPayload::data,
            ByteBufCodecs.STRING_UTF8, NoodlerExportResultPayload::errorKey,
            NoodlerExportResultPayload::new);

    public static NoodlerExportResultPayload success(int requestId, byte[] data) {
        return new NoodlerExportResultPayload(requestId, data, "");
    }

    public static NoodlerExportResultPayload failure(int requestId, String errorKey) {
        return new NoodlerExportResultPayload(requestId, new byte[0], errorKey);
    }

    public boolean isSuccess() {
        return this.errorKey.isEmpty();
    }

    @Override
    public Type<NoodlerExportResultPayload> type() {
        return TYPE;
    }
}
