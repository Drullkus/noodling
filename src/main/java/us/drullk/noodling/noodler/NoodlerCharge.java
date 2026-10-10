package us.drullk.noodling.noodler;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Marks a Noodler whose holder is holding left click. It stores the game time at which the charge started so both
 * sides can measure how long the charge has lasted. The component is removed when the button is released.
 *
 * <p>It lives in its own component so that the charge does not rewrite {@link NoodlerState} every time.
 *
 * <p>Written by Claude Code; conversation transcript: {@code llm/transcripts/transcript_2026-10-10_25b84f50-3f88-41a1-a942-98824ffe67dc.md}.
 */
public record NoodlerCharge(long startTick) {

    public static final Codec<NoodlerCharge> CODEC = Codec.LONG.xmap(NoodlerCharge::new, NoodlerCharge::startTick);
    public static final StreamCodec<ByteBuf, NoodlerCharge> STREAM_CODEC = ByteBufCodecs.VAR_LONG.map(NoodlerCharge::new, NoodlerCharge::startTick);

    /** Ticks the charge has lasted at the given game time, never negative. */
    public long ticksHeld(long gameTime) {
        return Math.max(0L, gameTime - this.startTick);
    }
}
