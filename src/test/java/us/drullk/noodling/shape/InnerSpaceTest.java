package us.drullk.noodling.shape;

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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link InnerSpaceIterable}: a single-block canary, uniqueness of world and inner positions, and
 * equivalence with a brute-force scan, for straight and bent spaces. The bend-specific tests check the straight
 * limit, the round trip between world and inner points, the geometry of an elbow, and the validity rules.
 *
 * <p>Written by Claude Code; conversation transcript: {@code llm/transcripts/transcript_2026-10-06_17647dd8-8de1-4b0b-b68d-89a2d0ee63a8.md}.
 * <p>Written by Claude Code; conversation transcript: {@code llm/transcripts/transcript_2026-10-09_5135373d-4acd-4a26-a410-3449a9a2abd5.md}.
 */
class InnerSpaceTest {

    /** The sample spaces that the property-style tests run over. */
    static Stream<Arguments> spaces() {
        List<Arguments> out = new ArrayList<>();
        out.add(Arguments.of("axis-aligned cornered", InnerSpaceIterable.cornered(new Vec3(10, 64, -7), new Quaterniond(), 3, 4, 5)));
        out.add(Arguments.of("axis-aligned centered on block centre", InnerSpaceIterable.centered(new Vec3(0.5, 0.5, 0.5), new Quaterniond(), 4, 4, 4)));
        out.add(Arguments.of("90 degrees about Y", InnerSpaceIterable.cornered(new Vec3(0, 0, 0), new Quaterniond().rotationY(Math.PI / 2), 2, 3, 7)));
        out.add(Arguments.of("diagonal noodle segment", InnerSpaceIterable.segment(new Vec3(0, 0, 0), new Vec3(40, 11, 23), 2, 2)));
        out.add(Arguments.of("vertical segment", InnerSpaceIterable.segment(new Vec3(1, 1, 1), new Vec3(1, 30, 1), 3, 3)));
        out.add(Arguments.of("scaled and sheared",
                InnerSpaceIterable.of(new Matrix4d().translation(3, 3, 3).rotateY(0.7).scale(2, 0.5, 3).m10(0.4), new Vector3d(-1, -1, -1), new Vector3d(1, 1, 1))));
        Random rng = new Random(0x9A57A);
        for (int i = 0; i < 12; i++) {
            Quaterniond q = new Quaterniond().rotationXYZ(rng.nextDouble() * 6.3, rng.nextDouble() * 6.3, rng.nextDouble() * 6.3);
            Vec3 c = new Vec3(rng.nextDouble() * 50 - 25, rng.nextDouble() * 50, rng.nextDouble() * 50 - 25);
            out.add(Arguments.of("random rotation " + i,
                    InnerSpaceIterable.centered(c, q, 1 + rng.nextDouble() * 10, 1 + rng.nextDouble() * 10, 1 + rng.nextDouble() * 24)));
        }
        out.add(Arguments.of("gentle bend",
                InnerSpaceIterable.centered(new Vec3(3.2, 70.1, -8.7), new Quaterniond().rotationXYZ(0.3, 1.2, 0.5), 6, 30, 6).bent(0.02)));
        out.add(Arguments.of("180 degree elbow", elbow()));
        out.add(Arguments.of("near-full turn, negative curvature",
                InnerSpaceIterable.centered(new Vec3(1.5, 2.5, 3.5), new Quaterniond().rotationY(0.4), 4, 40, 5).bent(-Math.toRadians(350) / 40)));
        out.add(Arguments.of("full turn",
                InnerSpaceIterable.centered(new Vec3(-20.3, 5.1, 9.9), new Quaterniond().rotationXYZ(0.7, 0.2, 1.1), 4, 40, 4).bent(2 * Math.PI / 40)));
        out.add(Arguments.of("bent on sheared base",
                InnerSpaceIterable.of(new Matrix4d().translation(3, 3, 3).rotateY(0.7).scale(2, 0.5, 3).m10(0.4), new Vector3d(-1, -1, -1), new Vector3d(1, 1, 1)).bent(0.4)));
        out.add(Arguments.of("bent cornered",
                InnerSpaceIterable.cornered(new Vec3(10, 64, -7), new Quaterniond().rotationX(0.9), 3, 20, 5).bent(-0.1)));
        for (int i = 0; i < 8; i++) {
            Quaterniond q = new Quaterniond().rotationXYZ(rng.nextDouble() * 6.3, rng.nextDouble() * 6.3, rng.nextDouble() * 6.3);
            Vec3 c = new Vec3(rng.nextDouble() * 50 - 25, rng.nextDouble() * 50, rng.nextDouble() * 50 - 25);
            double sizeX = 1 + rng.nextDouble() * 8;
            double sizeY = 4 + rng.nextDouble() * 30;
            double sizeZ = 1 + rng.nextDouble() * 8;
            // The bend radius sizeY / turn must stay above the half width sizeX / 2.
            double maxTurn = Math.min(1.95 * Math.PI, 1.8 * sizeY / sizeX);
            double turn = 0.1 + rng.nextDouble() * (maxTurn - 0.1);
            double curvature = (rng.nextBoolean() ? turn : -turn) / sizeY;
            out.add(Arguments.of("random bend " + i, InnerSpaceIterable.centered(c, q, sizeX, sizeY, sizeZ).bent(curvature)));
        }
        return out.stream();
    }

    /** A 12-wide, 60-long tube bent through 180 degrees: bend radius {@code 60 / PI}, centre at world {@code (R, 0, 0)}. */
    private static InnerSpaceIterable elbow() {
        return InnerSpaceIterable.centered(new Vec3(0, 0, 0), new Quaterniond(), 12, 60, 12).bent(Math.PI / 60);
    }

    // ---- Canary tests that place exactly 1 block ----------------------------------------------------------------

    @Test
    void unitCubeAtIntegerCornerPlacesExactlyOneBlock() {
        InnerSpaceIterable space = InnerSpaceIterable.cornered(new Vec3(5, 70, -3), new Quaterniond(), 1, 1, 1);
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
        InnerSpaceIterable space = InnerSpaceIterable.centered(new Vec3(-12.5, 80.5, 33.5), rotation, 1, 1, 1);
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
    void worldPositionsAreUnique(String name, InnerSpaceIterable space) {
        List<InnerSpacePos> placed = space.stream().toList();
        Set<BlockPos> seen = new HashSet<>();
        for (InnerSpacePos p : placed) {
            assertTrue(seen.add(p.toBlockPos()), () -> name + ": world position emitted twice: " + p);
        }
        assertFalse(placed.isEmpty(), name + ": space produced no blocks");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("spaces")
    void innerPositionsAreUniqueAndWellSeparated(String name, InnerSpaceIterable space) {
        List<InnerSpacePos> placed = space.stream().toList();

        // Distinct block centres are at least one block apart in world space. Pulling them into inner space
        // shrinks that gap by at most the largest stretch of the world-to-inner map, so every pair of inner
        // positions must stay at least this far apart. For pure rotations the floor is exactly 1.
        double floor = 1.0 / largestStretch(space.worldToInner().invertAffine(new Matrix4d())) * bendShrink(space);
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

    /** Smallest factor by which unbending shrinks a distance: {@code R / rho} on the outer wall of the bend, or 1 when straight. */
    private static double bendShrink(InnerSpaceIterable space) {
        if (space.curvature() == 0) {
            return 1;
        }
        double radius = Math.abs(1 / space.curvature());
        double halfWidth = Math.max(Math.abs(space.innerMin().x), Math.abs(space.innerMax().x));
        return radius / (radius + halfWidth);
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
    void matchesBruteForceAndReportsCorrectInnerCoordinates(String name, InnerSpaceIterable space) {
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
    // ---------------------------------------------------------------------------------------------------------
    // Bending

    @Test
    void zeroCurvatureIsTheStraightSpace() {
        InnerSpaceIterable straight = InnerSpaceIterable.centered(new Vec3(4.2, 9.1, -3.3), new Quaterniond().rotationXYZ(0.4, 1.1, 2.3), 5, 20, 5);
        assertTrue(straight.bent(0) == straight, "bent(0) on a straight space must return the same instance");

        InnerSpaceIterable unbent = straight.bent(0.05).bent(0);
        assertEquals(0.0, unbent.curvature());
        assertEquals(blockSet(straight), blockSet(unbent), "bent(0) on a bent space must restore the straight block set");
    }

    @Test
    void tinyCurvatureMatchesTheStraightSpace() {
        InnerSpaceIterable straight = InnerSpaceIterable.centered(new Vec3(4.2, 9.1, -3.3), new Quaterniond().rotationXYZ(0.4, 1.1, 2.3), 5, 20, 5);
        InnerSpaceIterable barelyBent = straight.bent(1e-7);

        List<InnerSpacePos> expected = straight.stream().toList();
        List<InnerSpacePos> actual = barelyBent.stream().toList();
        assertEquals(expected.size(), actual.size(), "a near-zero curvature must visit the same number of blocks");
        for (int i = 0; i < expected.size(); i++) {
            InnerSpacePos e = expected.get(i);
            InnerSpacePos a = actual.get(i);
            assertEquals(e.toBlockPos(), a.toBlockPos(), "block order must match the straight space");
            assertEquals(e.innerX(), a.innerX(), 1e-3);
            assertEquals(e.innerY(), a.innerY(), 1e-3);
            assertEquals(e.innerZ(), a.innerZ(), 1e-3);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("spaces")
    void innerAndWorldPointsRoundTrip(String name, InnerSpaceIterable space) {
        Random rng = new Random(name.hashCode());
        Vector3d min = space.innerMin();
        Vector3d max = space.innerMax();
        for (int i = 0; i < 200; i++) {
            Vector3d inner = new Vector3d(
                    min.x + rng.nextDouble() * (max.x - min.x),
                    min.y + rng.nextDouble() * (max.y - min.y),
                    min.z + rng.nextDouble() * (max.z - min.z));
            Vector3d world = space.toWorld(inner);
            Vector3d back = space.toInner(world);
            assertEquals(inner.x, back.x, 1e-6, () -> name + ": inner X of " + inner);
            assertEquals(inner.y, back.y, 1e-6, () -> name + ": inner Y of " + inner);
            assertEquals(inner.z, back.z, 1e-6, () -> name + ": inner Z of " + inner);

            Vector3d worldAgain = space.toWorld(back);
            assertEquals(world.x, worldAgain.x, 1e-6, () -> name + ": world X of " + world);
            assertEquals(world.y, worldAgain.y, 1e-6, () -> name + ": world Y of " + world);
            assertEquals(world.z, worldAgain.z, 1e-6, () -> name + ": world Z of " + world);
        }
    }

    @Test
    void elbowBendsThroughHalfATurn() {
        InnerSpaceIterable space = elbow();
        double radius = 60 / Math.PI;
        Vector3d centre = new Vector3d(radius, 0, 0);

        Vector3d start = space.toWorld(new Vector3d(0, -30, 0));
        Vector3d middle = space.toWorld(new Vector3d(0, 0, 0));
        Vector3d end = space.toWorld(new Vector3d(0, 30, 0));

        assertEquals(0.0, middle.distance(0, 0, 0), 1e-9, "the centre line passes through the world centre");
        assertEquals(radius, middle.distance(centre), 1e-9);
        assertEquals(radius, start.distance(centre), 1e-9);
        assertEquals(radius, end.distance(centre), 1e-9);
        assertEquals(2 * radius, start.distance(end), 1e-9, "the two ends of a half turn lie a diameter apart");
        assertEquals(radius, start.x, 1e-9);
        assertEquals(-radius, start.y, 1e-9, "negative inner Y turns clockwise toward +X");
        assertEquals(radius, end.x, 1e-9);
        assertEquals(radius, end.y, 1e-9);

        // The outer wall is farther from the bend centre, the inner wall closer; the cross-section keeps its width.
        assertEquals(radius + 5.5, space.toWorld(new Vector3d(-5.5, 17, 0)).distance(centre), 1e-9);
        assertEquals(radius - 5.5, space.toWorld(new Vector3d(5.5, 17, 0)).distance(centre), 1e-9);
        assertEquals(4.0, space.toWorld(new Vector3d(0, 17, 4)).z, 1e-9);
    }

    @Test
    void bentUnitCubeCentredOnBlockPlacesExactlyOneBlock() {
        InnerSpaceIterable space = InnerSpaceIterable.centered(new Vec3(-12.5, 80.5, 33.5), new Quaterniond().rotationXYZ(0.4, 1.1, 2.3), 1, 1, 1).bent(0.3);
        List<InnerSpacePos> placed = space.stream().toList();

        assertEquals(1, placed.size(), "bent 1x1x1 space must place exactly one block");
        InnerSpacePos only = placed.getFirst();
        assertEquals(new BlockPos(-13, 80, 33), only.toBlockPos());
        assertEquals(0.0, only.innerX(), 1e-6);
        assertEquals(0.0, only.innerY(), 1e-6);
        assertEquals(0.0, only.innerZ(), 1e-6);
    }

    @Test
    void rejectsABendRadiusInsideTheBox() {
        InnerSpaceIterable space = InnerSpaceIterable.centered(new Vec3(0, 0, 0), new Quaterniond(), 4, 10, 4);
        assertThrows(IllegalArgumentException.class, () -> space.bent(0.6), "radius 1.67 is inside the half width 2");
        assertThrows(IllegalArgumentException.class, () -> space.bent(-0.5), "radius 2 touches the half width 2");
        assertThrows(IllegalArgumentException.class, () -> space.bent(Double.NaN));
        space.bent(0.49); // radius just above the half width is fine
    }

    @Test
    void rejectsMoreThanOneFullTurn() {
        InnerSpaceIterable space = InnerSpaceIterable.centered(new Vec3(0, 0, 0), new Quaterniond(), 2, 10, 2);
        assertThrows(IllegalArgumentException.class, () -> space.bent(0.7), "10 * 0.7 radians is more than a full turn");
        space.bent(2 * Math.PI / 10); // exactly one turn is fine
    }

    @Test
    void acceptsAFullTurnComputedInFloat() {
        // Mth.TWO_PI is a float that overshoots 2 * Math.PI by about 2e-7, and placers divide it by a float height.
        float twoPi = (float) (2 * Math.PI);
        float height = 60.5f;
        InnerSpaceIterable space = InnerSpaceIterable.centered(new Vec3(0, 0, 0), new Quaterniond().rotationXYZ(0.4, 1.1, 2.3), 12, height, 12).bent(twoPi / height);
        assertFalse(space.stream().toList().isEmpty());
        assertEquals(blockSet(space), blockSet(space.bent(2 * Math.PI / height)), "float and double full turns visit the same blocks");
    }

    private static Set<BlockPos> blockSet(InnerSpaceIterable space) {
        Set<BlockPos> out = new HashSet<>();
        for (InnerSpacePos p : space) {
            out.add(p.toBlockPos());
        }
        return out;
    }
}
