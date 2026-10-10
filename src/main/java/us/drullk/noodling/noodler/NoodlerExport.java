package us.drullk.noodling.noodler;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Server-side export of the shape a Noodler currently shows as a compressed structure template (the same bytes a
 * {@code .nbt} file holds). The shape is regenerated from the item's state without rolling a fresh seed (see
 * {@link NoodlerPlacement#prepare}) and written into a {@link RecordingLevelWriter}, so the world is not touched
 * and the Noodler keeps the exported instance.
 *
 * <p>Written by Claude Code; conversation transcript: {@code llm/transcripts/transcript_2026-10-10_25b84f50-3f88-41a1-a942-98824ffe67dc.md}.
 */
public final class NoodlerExport {

    /** Custom payloads cap at 1 MiB; leave room for the payload header. */
    public static final int MAX_BYTES = 1_000_000;

    private NoodlerExport() {
    }

    /** Builds the template tag for the Noodler in {@code stack} and stores the resolved state back on it. */
    public static CompoundTag buildTemplate(ServerPlayer player, ItemStack stack) {
        NoodlerState state = stack.getOrDefault(NoodlerComponents.STATE.get(), NoodlerState.DEFAULT);
        NoodlerPlacement.Prepared prepared = NoodlerPlacement.prepare(state, player.level().getRandom(), true);
        RecordingLevelWriter recorder = new RecordingLevelWriter();
        prepared.shape().place(recorder, Vec3.ZERO, prepared.params());
        stack.set(NoodlerComponents.STATE.get(), prepared.nextState());
        return recorder.toTemplateTag();
    }

    /** Gzip-compresses a tag the way {@code NbtIo.writeCompressed} writes a file. */
    public static byte[] compress(CompoundTag tag) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        NbtIo.writeCompressed(tag, bytes);
        return bytes.toByteArray();
    }
}
