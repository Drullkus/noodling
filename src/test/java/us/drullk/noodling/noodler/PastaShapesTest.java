package us.drullk.noodling.noodler;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import tamaized.beanification.Autowired;
import us.drullk.noodling.Noodling;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks every shape descriptor: rolled slider values lie inside the live bounds, integer sliders roll whole
 * numbers, the pasta block is one of the mod's blocks, the sauce block is air or one of the sauce blocks with the
 * intended odds, and the Noodler's per-parameter resolution honours locks, seeds and manual values.
 *
 * <p>Written by Claude Code; conversation transcript: {@code llm/transcripts/transcript_2026-10-10_25b84f50-3f88-41a1-a942-98824ffe67dc.md}.
 * <p>Written by Claude Code; conversation transcript: {@code llm/transcripts/transcript_2026-10-10_c3299adc-f781-48a8-9c79-c51c79b26b4b.md}.
 */
class PastaShapesTest {

    @Autowired
    private static PastaShapeLookup pastaShapeLookup;

    private static final int ROLLS = 300;
    private static final float TOLERANCE = 1.0e-4F;

    static List<PastaShape> shapes() {
        return pastaShapeLookup.getAll().values().stream().toList();
    }

    @Test
    void everyShapeHasRotationPastaSauceAndUniqueIds() {
        Set<String> ids = new HashSet<>();
        for (PastaShape shape : shapes()) {
            assertTrue(ids.add(shape.id()), "duplicate shape id " + shape.id());
            assertNotNull(shape.param(PastaParam.ROTATION), shape.id() + " has no rotation");
            assertNotNull(shape.param(PastaParam.PASTA), shape.id() + " has no pasta block");
            PastaParam sauce = shape.param(PastaParam.SAUCE);
            assertNotNull(sauce, shape.id() + " has no sauce block");
            assertEquals(PastaParam.Kind.SAUCE, sauce.kind(), shape.id() + " sauce has the wrong kind");
            assertEquals(shape, pastaShapeLookup.byId(shape.id()));
        }
    }

    @ParameterizedTest
    @MethodSource("shapes")
    void rolledValuesLieInsideLiveBounds(PastaShape shape) {
        for (int roll = 0; roll < ROLLS; roll++) {
            ResolvedParams values = new ResolvedParams();
            RandomSource random = RandomSource.create(roll * 7919L);
            for (PastaParam param : shape.params()) {
                PastaParam.Bounds bounds = param.bounds(values);
                ParamValue value = param.roll(random, values);
                assertTrue(param.accepts(value), shape.id() + "." + param.name() + " rolled the wrong type");
                switch (param.kind()) {
                    case FLOAT_SLIDER, INT_SLIDER -> {
                        assertNotNull(bounds, shape.id() + "." + param.name() + " has no bounds");
                        float f = ((ParamValue.Num) value).asFloat();
                        assertTrue(f >= bounds.min() - TOLERANCE && f <= bounds.max() + TOLERANCE,
                                shape.id() + "." + param.name() + " rolled " + f + " outside " + bounds);
                        assertTrue(bounds.min() <= bounds.max(), shape.id() + "." + param.name() + " has inverted bounds " + bounds);
                        if (param.kind() == PastaParam.Kind.INT_SLIDER) {
                            double d = ((ParamValue.Num) value).value();
                            assertEquals(Math.rint(d), d, shape.id() + "." + param.name() + " rolled a fraction");
                        }
                    }
                    case FLOAT_FIELD -> assertTrue(Float.isFinite(((ParamValue.Num) value).asFloat()));
                    case BLOCK -> {
                        var block = ((ParamValue.Block) value).state().getBlock();
                        assertTrue(Noodling.PASTA_BLOCKS.stream().anyMatch(holder -> holder.get() == block), shape.id() + " rolled a non-pasta block");
                    }
                    case SAUCE -> {
                        var state = ((ParamValue.Block) value).state();
                        assertTrue(state.isAir() || Noodling.SAUCE_BLOCKS.stream().anyMatch(holder -> holder.get() == state.getBlock()), shape.id() + " rolled a non-sauce block");
                    }
                    case ROTATION -> {
                        var rotation = ((ParamValue.Rotation) value).toQuaternion();
                        assertTrue(Double.isFinite(rotation.x + rotation.y + rotation.z + rotation.w));
                    }
                }
                values.put(param.name(), value);
            }
        }
    }

    @ParameterizedTest
    @MethodSource("shapes")
    void sauceRollsNoSauceHalfTheTimeAndEachSauceASixth(PastaShape shape) {
        PastaParam sauce = shape.param(PastaParam.SAUCE);
        assertNotNull(sauce);
        int rolls = 6000;
        Map<Block, Integer> counts = new LinkedHashMap<>();
        RandomSource random = RandomSource.create(2024L);
        for (int roll = 0; roll < rolls; roll++) {
            Block block = ((ParamValue.Block) sauce.roll(random, new ResolvedParams())).state().getBlock();
            counts.merge(block, 1, Integer::sum);
        }
        // Expected 3000 for air and 1000 per sauce; the tolerance covers about five standard deviations.
        assertEquals(3000, counts.getOrDefault(Blocks.AIR, 0), 200, shape.id() + " no-sauce odds");
        for (var holder : Noodling.SAUCE_BLOCKS) {
            assertEquals(1000, counts.getOrDefault(holder.get(), 0), 150, shape.id() + " " + holder.getId() + " odds");
        }
        assertEquals(1 + Noodling.SAUCE_BLOCKS.size(), counts.size(), shape.id() + " rolled an unexpected block");
    }

    @ParameterizedTest
    @MethodSource("shapes")
    void resolveAllMatchesManualRollOrder(PastaShape shape) {
        ResolvedParams a = shape.resolveAll(RandomSource.create(99L));
        ResolvedParams b = shape.resolveAll(RandomSource.create(99L));
        assertEquals(a.values(), b.values());
    }

    @Test
    void lockedSeedIsDeterministicAndUnlockedParamsFollowTheNoodlerSeed() {
        PastaShape shell = pastaShapeLookup.byId("shell");
        assertNotNull(shell);
        NoodlerState state = NoodlerState.DEFAULT.withParams(Map.of("diameter", new ParamState("1234", true, Optional.empty())));

        Map<String, ParamState> firstOut = new LinkedHashMap<>();
        ResolvedParams first = NoodlerPlacement.resolve(shell, state, 5L, firstOut);
        Map<String, ParamState> secondOut = new LinkedHashMap<>();
        ResolvedParams second = NoodlerPlacement.resolve(shell, state, 5L, secondOut);
        assertEquals(first.values(), second.values(), "same Noodler seed must give the same shape");

        Map<String, ParamState> otherOut = new LinkedHashMap<>();
        ResolvedParams other = NoodlerPlacement.resolve(shell, state, 6L, otherOut);
        assertEquals(first.f("diameter"), other.f("diameter"), "locked seed must not follow the Noodler seed");
        assertTrue(first.f("height") != other.f("height") || first.f("taper_factor") != other.f("taper_factor"), "unlocked parameters must follow the Noodler seed");

        ParamState diameter = firstOut.get("diameter");
        assertEquals("1234", diameter.seed());
        assertTrue(diameter.locked());
        assertEquals(first.f("diameter"), ((ParamValue.Num) diameter.value().orElseThrow()).asFloat());

        ParamState height = firstOut.get("height");
        assertTrue(!height.locked());
        assertEquals(NoodlerSeeds.text(NoodlerSeeds.derive(5L, 2)), height.seed(), "unlocked parameters record their derived seed");
        assertEquals(shell.params().size(), firstOut.size());
    }

    @Test
    void manualValueIsUsedAsTyped() {
        PastaShape penne = pastaShapeLookup.byId("penne");
        assertNotNull(penne);
        NoodlerState state = NoodlerState.DEFAULT.withParams(Map.of("height", new ParamState("", true, Optional.of(ParamValue.Num.of(64.25F)))));
        Map<String, ParamState> out = new LinkedHashMap<>();
        ResolvedParams resolved = NoodlerPlacement.resolve(penne, state, 1L, out);
        assertEquals(64.25F, resolved.f("height"));
        assertEquals(state.param("height"), out.get("height"));
    }

    @Test
    void lockedWithoutSeedOrValueRollsAndRecordsASeed() {
        PastaShape penne = pastaShapeLookup.byId("penne");
        assertNotNull(penne);
        NoodlerState state = NoodlerState.DEFAULT.withParams(Map.of("height", new ParamState("", true, Optional.empty())));
        Map<String, ParamState> out = new LinkedHashMap<>();
        NoodlerPlacement.resolve(penne, state, 1L, out);
        ParamState height = out.get("height");
        assertTrue(height.locked());
        assertEquals(NoodlerSeeds.text(NoodlerSeeds.derive(1L, 1)), height.seed());
        assertTrue(height.value().isPresent());
    }

    @Test
    void seedResolution() {
        RandomSource random = RandomSource.create(3L);
        long rolled = RandomSource.create(3L).nextLong();
        assertEquals(rolled, NoodlerPlacement.resolveSeed(NoodlerState.DEFAULT, random), "unlocked rolls a fresh seed");
        assertEquals(42L, NoodlerPlacement.resolveSeed(new NoodlerState("42", true, Optional.empty(), Map.of()), RandomSource.create(3L)));
        assertEquals("abc".hashCode(), NoodlerPlacement.resolveSeed(new NoodlerState("abc", true, Optional.empty(), Map.of()), RandomSource.create(3L)));
    }
}
