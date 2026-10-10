package us.drullk.noodling.noodler;

import net.minecraft.world.level.levelgen.WorldOptions;

import java.util.OptionalLong;

/**
 * Seed text parsing and per-parameter seed derivation.
 *
 * <p>Seed fields accept the same text as the world creation screen: an empty field is no seed, a number is used as
 * is, and any other text is hashed. Each parameter of a shape gets its own seed derived from the Noodler's seed and
 * the parameter's position, so locking the Noodler's seed fixes every unlocked parameter too.
 *
 * <p>Written by Claude Code; conversation transcript: {@code llm/transcripts/transcript_2026-10-10_25b84f50-3f88-41a1-a942-98824ffe67dc.md}.
 */
public final class NoodlerSeeds {

    private NoodlerSeeds() {
    }

    /** Reads a seed field like {@code CreateWorldScreen} does; empty when the field is blank. */
    public static OptionalLong parse(String text) {
        return WorldOptions.parseSeed(text);
    }

    /** The text shown for a seed that the Noodler rolled. */
    public static String text(long seed) {
        return Long.toString(seed);
    }

    /**
     * Derives the seed of the parameter at {@code index} from the Noodler's seed. Uses the SplitMix64 finalizer so
     * nearby indices and seeds give unrelated results.
     */
    public static long derive(long seed, int index) {
        long h = seed + 0x9E3779B97F4A7C15L * (index + 1L);
        h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
        return h ^ (h >>> 31);
    }
}
