package us.drullk.noodling.noodler;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.OptionalLong;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks seed field parsing (world creation screen rules) and the derivation of per-parameter seeds.
 *
 * <p>Written by Claude Code; conversation transcript: {@code llm/transcripts/transcript_2026-10-10_25b84f50-3f88-41a1-a942-98824ffe67dc.md}.
 */
class NoodlerSeedsTest {

    @Test
    void blankSeedIsAbsent() {
        assertEquals(OptionalLong.empty(), NoodlerSeeds.parse(""));
        assertEquals(OptionalLong.empty(), NoodlerSeeds.parse("   "));
    }

    @Test
    void numericSeedIsUsedAsIs() {
        assertEquals(OptionalLong.of(-42L), NoodlerSeeds.parse(" -42 "));
        assertEquals(OptionalLong.of(Long.MAX_VALUE), NoodlerSeeds.parse(Long.toString(Long.MAX_VALUE)));
    }

    @Test
    void textSeedIsHashed() {
        assertEquals(OptionalLong.of("spaghetti".hashCode()), NoodlerSeeds.parse("spaghetti"));
    }

    @Test
    void rolledSeedTextRoundTrips() {
        long seed = 0x5DEECE66DL;
        assertEquals(OptionalLong.of(seed), NoodlerSeeds.parse(NoodlerSeeds.text(seed)));
    }

    @Test
    void derivedSeedsAreStable() {
        assertEquals(NoodlerSeeds.derive(123L, 4), NoodlerSeeds.derive(123L, 4));
    }

    @Test
    void derivedSeedsAreDistinctAcrossIndicesAndSeeds() {
        Set<Long> seen = new HashSet<>();
        for (long seed = -3; seed <= 3; seed++) {
            for (int index = 0; index < 32; index++) {
                assertTrue(seen.add(NoodlerSeeds.derive(seed, index)), "collision at seed " + seed + " index " + index);
            }
        }
        assertNotEquals(NoodlerSeeds.derive(0L, 0), NoodlerSeeds.derive(0L, 1));
    }
}
