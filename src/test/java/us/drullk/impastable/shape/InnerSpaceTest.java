package us.drullk.impastable.shape;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Stream;

import org.joml.Matrix3d;
import org.joml.Matrix4d;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link InnerSpace}: a single-block canary, uniqueness of world and inner positions, and
 * equivalence with a brute-force scan.
 *
 * <p>Written by Claude Code; conversation transcript: {@code llm/transcripts/transcript-2026-10-06-17647dd8-8de1-4b0b-b68d-89a2d0ee63a8.md}.
 */
class InnerSpaceTest {

    /** The sample spaces that the property-style tests run over. */
    static Stream<Arguments> spaces() {
        List<Arguments> out = new ArrayList<>();
        out.add(Arguments.of("axis-aligned cornered", InnerSpace.cornered(new Vec3(10, 64, -7), new Quaterniond(), 3, 4, 5)));
        out.add(Arguments.of("axis-aligned centered on block centre", InnerSpace.centered(new Vec3(0.5, 0.5, 0.5), new Quaterniond(), 4, 4, 4)));
        out.add(Arguments.of("90 degrees about Y", InnerSpace.cornered(new Vec3(0, 0, 0), new Quaterniond().rotationY(Math.PI / 2), 2, 3, 7)));
        out.add(Arguments.of("diagonal noodle segment", InnerSpace.segment(new Vec3(0, 0, 0), new Vec3(40, 11, 23), 2, 2)));
        out.add(Arguments.of("vertical segment", InnerSpace.segment(new Vec3(1, 1, 1), new Vec3(1, 30, 1), 3, 3)));
        out.add(Arguments.of("scaled and sheared",
                InnerSpace.of(new Matrix4d().translation(3, 3, 3).rotateY(0.7).scale(2, 0.5, 3).m10(0.4), new Vector3d(-1, -1, -1), new Vector3d(1, 1, 1))));
        Random rng = new Random(0x9A57A);
        for (int i = 0; i < 12; i++) {
            Quaterniond q = new Quaterniond().rotationXYZ(rng.nextDouble() * 6.3, rng.nextDouble() * 6.3, rng.nextDouble() * 6.3);
            Vec3 c = new Vec3(rng.nextDouble() * 50 - 25, rng.nextDouble() * 50, rng.nextDouble() * 50 - 25);
            out.add(Arguments.of("random rotation " + i,
                    InnerSpace.centered(c, q, 1 + rng.nextDouble() * 10, 1 + rng.nextDouble() * 10, 1 + rng.nextDouble() * 24)));
        }
        return out.stream();
    }

    // ---- Canary tests that place exactly 1 block ----------------------------------------------------------------

    @Test
    void unitCubeAtIntegerCornerPlacesExactlyOneBlock() {
        InnerSpace space = InnerSpace.cornered(new Vec3(5, 70, -3), new Quaterniond(), 1, 1, 1);
        List<InnerSpacePos> placed = space.stream().toList();

        assertEquals(1, placed.size(), "a 1x1x1 space must place exactly one block");
        InnerSpacePos only = placed.getFirst();
        assertEquals(new BlockPos(5, 70, -3), only.toBlockPos());
        assertEquals(0.5f, only.innerX());
        assertEquals(0.5f, only.innerY());
        assertEquals(0.5f, only.innerZ());
        assertTrue(space.containsBlock(5, 70, -3));
        assertFalse(space.containsBlock(6, 70, -3));
    }

    @ParameterizedTest(name = "rotation {0}")
    @MethodSource("unitCubeRotations")
    void rotatedUnitCubeCentredOnBlockPlacesExactlyOneBlock(String name, Quaterniond rotation) {
        // The block centre maps to inner (0, 0, 0), which lies inside [-0.5, 0.5) for any rotation; every other
        // block centre is at least one block away and so lands outside the half-unit box.
        InnerSpace space = InnerSpace.centered(new Vec3(-12.5, 80.5, 33.5), rotation, 1, 1, 1);
        List<InnerSpacePos> placed = space.stream().toList();

        assertEquals(1, placed.size(), "rotated 1x1x1 space must place exactly one block");
        InnerSpacePos only = placed.getFirst();
        assertEquals(new BlockPos(-13, 80, 33), only.toBlockPos());
        assertEquals(0.0, only.innerX(), 1e-6);
        assertEquals(0.0, only.innerY(), 1e-6);
        assertEquals(0.0, only.innerZ(), 1e-6);
    }

    static Stream<Arguments> unitCubeRotations() {
        return Stream.of(
                Arguments.of("identity", new Quaterniond()),
                Arguments.of("45 degrees about Y", new Quaterniond().rotationY(Math.PI / 4)),
                Arguments.of("arbitrary", new Quaterniond().rotationXYZ(0.4, 1.1, 2.3)),
                Arguments.of("cube diagonal up", new Quaterniond().rotationTo(new Vector3d(1, 1, 1).normalize(), new Vector3d(0, 1, 0))));
    }

    // ---- Enforce uniqueness ----------------------------------------------------------------

    @ParameterizedTest(name = "{0}")
    @MethodSource("spaces")
    void worldPositionsAreUnique(String name, InnerSpace space) {
        List<InnerSpacePos> placed = space.stream().toList();
        Set<BlockPos> seen = new HashSet<>();
        for (InnerSpacePos p : placed) {
            assertTrue(seen.add(p.toBlockPos()), () -> name + ": world position emitted twice: " + p);
        }
        assertFalse(placed.isEmpty(), name + ": space produced no blocks");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("spaces")
    void innerPositionsAreUniqueAndWellSeparated(String name, InnerSpace space) {
        List<InnerSpacePos> placed = space.stream().toList();

        // Distinct block centres are at least one block apart in world space. Pulling them into inner space
        // shrinks that gap by at most the largest stretch of the world-to-inner map, so every pair of inner
        // positions must stay at least this far apart. For pure rotations the floor is exactly 1.
        double floor = 1.0 / largestStretch(space.worldToInner().invertAffine(new Matrix4d()));
        double tolerance = floor * 1e-4;

        double closest = Double.POSITIVE_INFINITY;
        for (int i = 0; i < placed.size(); i++) {
            InnerSpacePos a = placed.get(i);
            for (int j = i + 1; j < placed.size(); j++) {
                InnerSpacePos b = placed.get(j);
                double dx = a.innerX() - b.innerX();
                double dy = a.innerY() - b.innerY();
                double dz = a.innerZ() - b.innerZ();
                double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (distance < closest) {
                    closest = distance;
                }
                assertFalse(a.innerX() == b.innerX() && a.innerY() == b.innerY() && a.innerZ() == b.innerZ(),
                        () -> name + ": identical inner position emitted twice: " + a + " and " + b);
                assertTrue(distance >= floor - tolerance,
                        () -> name + ": inner positions anomalously close (" + distance + " < " + floor + "): " + a + " and " + b);
            }
        }
        if (placed.size() > 1) {
            assertTrue(Double.isFinite(closest), name + ": expected at least one pair");
        }
    }

    /** Largest factor by which the linear part of {@code m} can stretch a vector (its spectral norm, via power iteration on M^T M). */
    private static double largestStretch(Matrix4d m) {
        Matrix3d a = m.get3x3(new Matrix3d());
        Matrix3d ata = a.transpose(new Matrix3d()).mul(a);
        Vector3d v = new Vector3d(0.6, 0.7, 0.4).normalize();
        double eigen = 0;
        for (int i = 0; i < 200; i++) {
            ata.transform(v);
            eigen = v.length();
            v.div(eigen);
        }
        return Math.sqrt(eigen);
    }

    // ---------------------------------------------------------------------------------------------------------
    // Correctness against brute force: the iterator visits exactly the blocks whose centre maps into the box.

    @ParameterizedTest(name = "{0}")
    @MethodSource("spaces")
    void matchesBruteForceAndReportsCorrectInnerCoordinates(String name, InnerSpace space) {
        Set<BlockPos> expected = new HashSet<>();
        Vector3d lo = space.worldBlockMin();
        Vector3d hi = space.worldBlockMax();
        Vector3d min = space.innerMin();
        Vector3d max = space.innerMax();
        for (int x = (int) lo.x - 2; x <= hi.x + 2; x++) {
            for (int y = (int) lo.y - 2; y <= hi.y + 2; y++) {
                for (int z = (int) lo.z - 2; z <= hi.z + 2; z++) {
                    Vector3d inner = space.toInner(new Vector3d(x + 0.5, y + 0.5, z + 0.5));
                    if (inner.x >= min.x && inner.x < max.x && inner.y >= min.y && inner.y < max.y && inner.z >= min.z && inner.z < max.z) {
                        expected.add(new BlockPos(x, y, z));
                    }
                }
            }
        }

        Set<BlockPos> actual = new HashSet<>();
        for (InnerSpacePos p : space) {
            actual.add(p.toBlockPos());
            Vector3d inner = space.toInner(new Vector3d(p.worldX() + 0.5, p.worldY() + 0.5, p.worldZ() + 0.5));
            assertEquals(inner.x, p.innerX(), 1e-4, () -> name + ": innerX of " + p);
            assertEquals(inner.y, p.innerY(), 1e-4, () -> name + ": innerY of " + p);
            assertEquals(inner.z, p.innerZ(), 1e-4, () -> name + ": innerZ of " + p);
        }
        assertEquals(expected, actual, name + ": block set differs from brute force");
    }
}
