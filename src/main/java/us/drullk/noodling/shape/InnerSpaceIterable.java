package us.drullk.noodling.shape;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.joml.Matrix4d;
import org.joml.Matrix4dc;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.jspecify.annotations.Nullable;

import net.minecraft.world.phys.Vec3;

/**
 * An oriented cuboid in world space that carries its own bounded "inner" coordinate system.
 *
 * <p>The relationship between the two spaces is affine: {@code world = innerToWorld * inner}. The inner box is the
 * half-open region {@code [min, max)} on each inner axis. A world block {@code (bx, by, bz)} belongs to the space
 * exactly when its centre {@code (bx + 0.5, by + 0.5, bz + 0.5)} maps into that box, which gives every block a
 * single, unambiguous inner coordinate (the inverse affine map applied to its centre).
 *
 * <h2>Traversal</h2>
 * Iterating yields every such block exactly once, with no duplicates and no gaps. The traversal is Bresenham/DDA
 * lifted from a line to a volume:
 * <ul>
 *   <li>Two world axes (the <em>outer</em> axes) are looped over the world-space bounding box of the cuboid.</li>
 *   <li>Along the remaining <em>scan</em> axis the inner coordinates are an affine function of the block index
 *       {@code k}: {@code inner(k) = rowStart + k * step}, where {@code step} is a constant column of the
 *       world-to-inner matrix. The first and last {@code k} inside the inner box are solved in closed form
 *       (a slab test against the six inner bounds), so no block outside the cuboid is ever visited on that axis,
 *       and the inner coordinates are advanced by a fixed delta per block rather than recomputed.</li>
 *   <li>The scan axis is the world axis along which the bounding box is longest, which minimises the number of
 *       rows (and therefore the number of empty rows for long thin cuboids such as a noodle lying diagonally).</li>
 * </ul>
 * Positions are produced in ascending order of outer axis A, then outer axis B, then the scan axis, where
 * A &lt; B &lt; scan are the world axes in x, y, z order with the scan axis removed.
 *
 * <h2>Bending</h2>
 * {@link #bent(double)} bends the inner Y axis into a circular arc with the given signed curvature (the inverse
 * of the bend radius, in blocks⁻¹). A positive curvature turns the +Y end of the axis toward +X, a negative one
 * toward -X, and the total turn is {@code (max.y - min.y) * curvature} radians. {@link #bentByTurn(double)} and
 * {@link #bentByRadius(double)} take that total turn or that bend radius instead and derive the curvature. The inner coordinates a bent
 * space reports are still the <em>straight</em> ones: {@code innerY} is the arc length along the centre line and
 * {@code (innerX, innerZ)} is the undistorted cross-section, so a shape function written for a straight space
 * produces the same shape, bent. The bend radius must be larger than the distance from the inner Y axis to the
 * box face it bends toward, and the box must not turn through more than one full circle.
 *
 * <p>A bent space keeps the scan above: along a row the affine coordinates still advance by a fixed delta, and
 * the runs of blocks inside the bent box (a torus sector) are solved in closed form, so no block outside the bent
 * box is visited. Each visited block then costs one square root and one {@code atan2} to unbend.
 *
 * <p>Instances are immutable and safe to iterate concurrently.
 *
 * <p>Written by Claude Code; conversation transcript: {@code llm/transcripts/transcript_2026-10-06_17647dd8-8de1-4b0b-b68d-89a2d0ee63a8.md}.
 * <p>Written by Claude Code; conversation transcript: {@code llm/transcripts/transcript_2026-10-09_5135373d-4acd-4a26-a410-3449a9a2abd5.md}.
 */
public final class InnerSpaceIterable implements Iterable<InnerSpacePos> {

    /** Below this magnitude an inner-coordinate delta along the scan axis is treated as zero. */
    private static final double ZERO_EPSILON = 1e-9;

    private final Matrix4d innerToWorld;
    private final Matrix4d worldToInner;
    private final double[] min = new double[3];
    private final double[] max = new double[3];
    /** Bend of the inner Y axis, or {@code null} for a straight space. */
    private final @Nullable Bend bend;

    /** {@code delta[worldAxis][innerComponent]}: change of inner coordinates when moving one block along a world axis. */
    private final double[][] delta = new double[3][3];
    /** Inner coordinates of the centre of world block (0, 0, 0). */
    private final double[] base = new double[3];
    /** Inclusive block-index bounds of the world-space AABB of the cuboid, per world axis. */
    private final int[] blockMin = new int[3];
    private final int[] blockMax = new int[3];
    private final int scanAxis;
    private final int outerA;
    private final int outerB;

    private InnerSpaceIterable(Matrix4dc innerToWorld, Vector3dc innerMin, Vector3dc innerMax, @Nullable Bend bend) {
        this.innerToWorld = new Matrix4d(innerToWorld);
        this.min[0] = innerMin.x(); this.min[1] = innerMin.y(); this.min[2] = innerMin.z();
        this.max[0] = innerMax.x(); this.max[1] = innerMax.y(); this.max[2] = innerMax.z();
        for (int j = 0; j < 3; j++) {
            if (!(min[j] < max[j])) {
                throw new IllegalArgumentException("Inner bounds must satisfy min < max on every axis, got min=" + innerMin + " max=" + innerMax);
            }
        }
        if (Math.abs(this.innerToWorld.determinant3x3()) < 1e-12) {
            throw new IllegalArgumentException("innerToWorld is not invertible: " + innerToWorld);
        }
        this.worldToInner = new Matrix4d(this.innerToWorld).invertAffine();
        this.bend = bend;

        // Columns of the linear part of worldToInner: how inner coords change per unit step on each world axis.
        for (int axis = 0; axis < 3; axis++) {
            for (int j = 0; j < 3; j++) {
                delta[axis][j] = worldToInner.get(axis, j);
            }
        }
        Vector3d origin = worldToInner.transformPosition(new Vector3d(0.5, 0.5, 0.5));
        base[0] = origin.x; base[1] = origin.y; base[2] = origin.z;

        // World-space AABB of the eight corners of the (bent) inner box, widened to whole blocks.
        double[] cornerMin = min;
        double[] cornerMax = max;
        if (bend != null) {
            cornerMin = new double[3];
            cornerMax = new double[3];
            bend.bentBounds(cornerMin, cornerMax);
        }
        double[] worldMin = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY};
        double[] worldMax = {Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY};
        Vector3d corner = new Vector3d();
        for (int c = 0; c < 8; c++) {
            corner.set((c & 1) == 0 ? cornerMin[0] : cornerMax[0], (c & 2) == 0 ? cornerMin[1] : cornerMax[1], (c & 4) == 0 ? cornerMin[2] : cornerMax[2]);
            this.innerToWorld.transformPosition(corner);
            for (int axis = 0; axis < 3; axis++) {
                double v = corner.get(axis);
                worldMin[axis] = Math.min(worldMin[axis], v);
                worldMax[axis] = Math.max(worldMax[axis], v);
            }
        }
        int longest = 0;
        for (int axis = 0; axis < 3; axis++) {
            // Block n has centre n + 0.5; padding by a block on each side guards against rounding at the faces.
            blockMin[axis] = (int) Math.floor(worldMin[axis] - 0.5);
            blockMax[axis] = (int) Math.ceil(worldMax[axis] - 0.5);
            if (blockMax[axis] - blockMin[axis] > blockMax[longest] - blockMin[longest]) {
                longest = axis;
            }
        }
        this.scanAxis = longest;
        this.outerA = longest == 0 ? 1 : 0;
        this.outerB = longest == 2 ? 1 : 2;
    }

    // ---------------------------------------------------------------------------------------------------------
    // Factories

    /**
     * General form: any invertible affine {@code innerToWorld} (rotation, scale, shear, translation) and an inner
     * box {@code [innerMin, innerMax)}.
     */
    public static InnerSpaceIterable of(Matrix4dc innerToWorld, Vector3dc innerMin, Vector3dc innerMax) {
        return new InnerSpaceIterable(innerToWorld, innerMin, innerMax, null);
    }

    /**
     * A cuboid of the given size (in blocks) whose inner origin sits at {@code worldCenter}; inner coordinates run
     * over {@code [-size/2, size/2)} on each axis, so shape functions can treat {@code (0,0,0)} as the middle.
     */
    public static InnerSpaceIterable centered(Vec3 worldCenter, Quaterniondc rotation, double sizeX, double sizeY, double sizeZ) {
        Matrix4d m = new Matrix4d().translation(worldCenter.x, worldCenter.y, worldCenter.z).rotate(rotation);
        return new InnerSpaceIterable(m, new Vector3d(-sizeX / 2, -sizeY / 2, -sizeZ / 2), new Vector3d(sizeX / 2, sizeY / 2, sizeZ / 2), null);
    }

    /**
     * A cuboid of the given size (in blocks) whose inner origin sits at {@code worldOrigin} and whose inner
     * coordinates run over {@code [0, size)} on each axis.
     */
    public static InnerSpaceIterable cornered(Vec3 worldOrigin, Quaterniondc rotation, double sizeX, double sizeY, double sizeZ) {
        Matrix4d m = new Matrix4d().translation(worldOrigin.x, worldOrigin.y, worldOrigin.z).rotate(rotation);
        return new InnerSpaceIterable(m, new Vector3d(0, 0, 0), new Vector3d(sizeX, sizeY, sizeZ), null);
    }

    /**
     * The volumetric analogue of a Bresenham line: a cuboid whose inner Z axis runs from {@code from} to {@code to}
     * (inner Z in {@code [0, length)}), with inner X spanning {@code width} and inner Y spanning {@code height}
     * centred on that line. Inner Y is chosen to point as far "up" (world +Y) as the direction allows, so for a
     * mostly horizontal noodle inner Y is its vertical thickness.
     */
    public static InnerSpaceIterable segment(Vec3 from, Vec3 to, double width, double height) {
        Vector3d w = new Vector3d(to.x - from.x, to.y - from.y, to.z - from.z);
        double length = w.length();
        if (length < 1e-9) {
            throw new IllegalArgumentException("segment endpoints coincide: " + from);
        }
        w.div(length);
        Vector3d up = Math.abs(w.y) < 0.99 ? new Vector3d(0, 1, 0) : new Vector3d(1, 0, 0);
        Vector3d u = up.cross(w, new Vector3d()).normalize(); // sideways
        Vector3d v = w.cross(u, new Vector3d());              // "up"-ish; (u, v, w) is right-handed
        Matrix4d m = new Matrix4d(
                u.x, u.y, u.z, 0,
                v.x, v.y, v.z, 0,
                w.x, w.y, w.z, 0,
                from.x, from.y, from.z, 1);
        return new InnerSpaceIterable(m, new Vector3d(-width / 2, -height / 2, 0), new Vector3d(width / 2, height / 2, length), null);
    }

    /**
     * A copy of this space whose inner Y axis bends into a circular arc; see the class description. The copy keeps
     * the affine map and the inner box of this space and replaces any earlier bend. A curvature of {@code 0} gives
     * a straight space.
     *
     * @param curvature signed curvature of the inner Y axis in blocks⁻¹ (the inverse of the bend radius)
     * @throws IllegalArgumentException when the bend radius does not clear the inner box on X, or the box turns
     *                                  through more than one full circle
     */
    public InnerSpaceIterable bent(double curvature) {
        if (curvature == 0) {
            return bend == null ? this : new InnerSpaceIterable(innerToWorld, innerMin(), innerMax(), null);
        }
        return new InnerSpaceIterable(innerToWorld, innerMin(), innerMax(), new Bend(curvature, min, max));
    }

    /**
     * {@link #bent(double)} expressed as the total angle the inner Y axis turns through over the length of the inner
     * box: the curvature is {@code turn / (max.y - min.y)}. A half turn ({@code PI}) makes an elbow and a full turn
     * ({@code 2 * PI}) closes the box into a ring.
     *
     * @param turn signed total turn in radians; positive turns toward +X
     */
    public InnerSpaceIterable bentByTurn(double turn) {
        return bent(turn / (max[1] - min[1]));
    }

    /**
     * {@link #bent(double)} expressed as the bend radius of the inner Y axis in blocks: the curvature is
     * {@code 1 / radius}. The radius must be larger than the distance from the inner Y axis to the box face it
     * bends toward, and {@code (max.y - min.y) / radius} must not exceed a full turn.
     *
     * @param radius signed bend radius in blocks; positive bends toward +X, and {@code 0} is rejected
     */
    public InnerSpaceIterable bentByRadius(double radius) {
        if (radius == 0) {
            throw new IllegalArgumentException("bend radius must be non-zero");
        }
        return bent(1 / radius);
    }

    // ---------------------------------------------------------------------------------------------------------
    // Queries

    public Matrix4dc innerToWorld() {
        return innerToWorld;
    }

    public Matrix4dc worldToInner() {
        return worldToInner;
    }

    public Vector3d innerMin() {
        return new Vector3d(min[0], min[1], min[2]);
    }

    public Vector3d innerMax() {
        return new Vector3d(max[0], max[1], max[2]);
    }

    /** Signed curvature of the inner Y axis in blocks⁻¹; {@code 0} for a straight space. */
    public double curvature() {
        return bend == null ? 0 : bend.curvature;
    }

    /** Inner coordinates of the centre of the given world block. */
    public Vector3d toInner(int worldX, int worldY, int worldZ) {
        Vector3d p = new Vector3d(
                base[0] + worldX * delta[0][0] + worldY * delta[1][0] + worldZ * delta[2][0],
                base[1] + worldX * delta[0][1] + worldY * delta[1][1] + worldZ * delta[2][1],
                base[2] + worldX * delta[0][2] + worldY * delta[1][2] + worldZ * delta[2][2]);
        if (bend != null) {
            bend.toStraight(p);
        }
        return p;
    }

    /** Inner coordinates of an arbitrary world point. */
    public Vector3d toInner(Vector3dc world) {
        Vector3d p = worldToInner.transformPosition(world, new Vector3d());
        if (bend != null) {
            bend.toStraight(p);
        }
        return p;
    }

    /** World coordinates of an inner point. */
    public Vector3d toWorld(Vector3dc inner) {
        Vector3d p = new Vector3d(inner);
        if (bend != null) {
            bend.toBent(p);
        }
        return innerToWorld.transformPosition(p);
    }

    /** Whether the given world block would be produced by {@link #iterator()}. */
    public boolean containsBlock(int worldX, int worldY, int worldZ) {
        Vector3d p = toInner(worldX, worldY, worldZ);
        return p.x >= min[0] && p.x < max[0]
                && p.y >= min[1] && p.y < max[1]
                && p.z >= min[2] && p.z < max[2];
    }

    /** Inclusive lower corner (block indices) of the world-space bounding box, padded by up to one block. */
    public Vector3d worldBlockMin() {
        return new Vector3d(blockMin[0], blockMin[1], blockMin[2]);
    }

    /** Inclusive upper corner (block indices) of the world-space bounding box, padded by up to one block. */
    public Vector3d worldBlockMax() {
        return new Vector3d(blockMax[0], blockMax[1], blockMax[2]);
    }

    // ---------------------------------------------------------------------------------------------------------
    // Traversal

    @Override
    public Iterator<InnerSpacePos> iterator() {
        return new ScanIterator();
    }

    public Stream<InnerSpacePos> stream() {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator(),
                Spliterator.ORDERED | Spliterator.DISTINCT | Spliterator.NONNULL | Spliterator.IMMUTABLE), false);
    }

    /**
     * Scan-line iterator. State is the current row {@code (a, b)} on the two outer world axes and the next block
     * index {@code k} on the scan axis; {@code kEnd} is the last block of the current run that lies inside the
     * inner box. A straight row has one run; a bent row may have several, queued in {@code intervals}.
     */
    private final class ScanIterator implements Iterator<InnerSpacePos> {
        private final double[] step = delta[scanAxis];
        private final double[] rowStart = new double[3];
        private final int[] intervals = new int[2 * Bend.MAX_INTERVALS];
        private final double[] scratch = new double[Bend.SCRATCH_SIZE];
        private final double[] point = new double[3];
        private int intervalCount;
        private int intervalIndex;
        private int a;
        private int b;
        private int k;
        private int kEnd = -1; // start on an exhausted (empty) row so the first advance() computes a real one
        private boolean done;
        private @Nullable InnerSpacePos next;

        ScanIterator() {
            a = blockMin[outerA];
            b = blockMin[outerB] - 1;
            k = 0;
            done = blockMin[outerA] > blockMax[outerA] || blockMin[outerB] > blockMax[outerB];
            advance();
        }

        @Override
        public boolean hasNext() {
            return next != null;
        }

        @Override
        public InnerSpacePos next() {
            InnerSpacePos out = next;
            if (out == null) {
                throw new NoSuchElementException();
            }
            advance();
            return out;
        }

        private void advance() {
            while (true) {
                while (k > kEnd) {
                    if (intervalIndex < intervalCount) {
                        k = intervals[2 * intervalIndex];
                        kEnd = intervals[2 * intervalIndex + 1];
                        intervalIndex++;
                        continue;
                    }
                    if (done) {
                        next = null;
                        return;
                    }
                    if (++b > blockMax[outerB]) {
                        b = blockMin[outerB];
                        if (++a > blockMax[outerA]) {
                            done = true;
                            next = null;
                            return;
                        }
                    }
                    computeRow();
                }
                int wx = scanAxis == 0 ? k : outerA == 0 ? a : b;
                int wy = scanAxis == 1 ? k : outerA == 1 ? a : b;
                int wz = scanAxis == 2 ? k : outerB == 2 ? b : a;
                if (bend == null) {
                    next = new InnerSpacePos(wx, wy, wz,
                            (float) (rowStart[0] + k * step[0]),
                            (float) (rowStart[1] + k * step[1]),
                            (float) (rowStart[2] + k * step[2]));
                    k++;
                    return;
                }
                // A bent run is conservative by a tiny margin at its ends, so test the unbent point exactly.
                point[0] = rowStart[0] + k * step[0];
                point[1] = rowStart[1] + k * step[1];
                point[2] = rowStart[2] + k * step[2];
                bend.toStraight(point);
                k++;
                if (point[0] >= min[0] && point[0] < max[0]
                        && point[1] >= min[1] && point[1] < max[1]
                        && point[2] >= min[2] && point[2] < max[2]) {
                    next = new InnerSpacePos(wx, wy, wz, (float) point[0], (float) point[1], (float) point[2]);
                    return;
                }
            }
        }

        /**
         * Slab test: inner(k) = rowStart + k * step must satisfy min_j <= inner_j(k) < max_j for all j. Each
         * inequality bounds k on one side; intersecting them gives the inclusive integer range [k, kEnd].
         * A bent space slab-tests only Z and hands X and Y to {@link Bend#rowIntervals}.
         */
        private void computeRow() {
            for (int j = 0; j < 3; j++) {
                rowStart[j] = base[j] + a * delta[outerA][j] + b * delta[outerB][j];
            }
            int lo = blockMin[scanAxis];
            int hi = blockMax[scanAxis];
            for (int j = bend == null ? 0 : 2; j < 3 && lo <= hi; j++) {
                double s = rowStart[j];
                double d = step[j];
                if (Math.abs(d) < ZERO_EPSILON) {
                    if (s < min[j] || s >= max[j]) {
                        lo = 1; hi = 0; // this row never enters the box on component j
                    }
                    continue;
                }
                double tMin = (min[j] - s) / d; // k at which inner_j == min_j
                double tMax = (max[j] - s) / d; // k at which inner_j == max_j
                if (d > 0) {
                    lo = Math.max(lo, (int) Math.ceil(tMin));       // k >= tMin
                    hi = Math.min(hi, (int) Math.ceil(tMax) - 1);   // k <  tMax
                } else {
                    hi = Math.min(hi, (int) Math.floor(tMin));      // k <= tMin
                    lo = Math.max(lo, (int) Math.floor(tMax) + 1);  // k >  tMax
                }
            }
            intervalIndex = 0;
            if (bend == null || lo > hi) {
                intervalCount = 0;
                k = lo;
                kEnd = hi;
                return;
            }
            intervalCount = bend.rowIntervals(rowStart, step, lo, hi, scratch, intervals);
            k = 1;
            kEnd = 0; // exhausted; advance() pulls the first queued run
        }
    }

    @Override
    public String toString() {
        return "InnerSpace[inner=" + innerMin() + "..." + innerMax() + ", innerToWorld=" + innerToWorld + (bend == null ? "" : ", " + bend) + "]";
    }
}
