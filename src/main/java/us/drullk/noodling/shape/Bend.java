package us.drullk.noodling.shape;

import org.joml.Vector3d;
import org.jspecify.annotations.Nullable;

/**
 * Bends the inner Y axis of an {@link InnerSpaceIterable} into a circular arc.
 *
 * <p>A bend has a signed curvature {@code k} in blocks⁻¹ and a bend radius {@code R = 1 / k}. A positive curvature
 * turns the +Y end of the inner axis toward +X. A negative curvature turns it toward -X. The centre of the bend
 * is the straight point {@code (R, 0, z)}.
 *
 * <p>Two coordinate systems meet here:
 * <ul>
 *   <li><em>Straight</em> coordinates {@code (x, y, z)} are the ones a shape function sees. {@code y} is the arc
 *       length along the centre line and {@code (x, z)} is the cross-section, which the bend does not distort.</li>
 *   <li><em>Bent</em> coordinates {@code (X, Y, Z)} are the ones the affine map of the space produces.</li>
 * </ul>
 * The map from straight to bent is
 * <pre>
 *     t = k * y
 *     X = R - (R - x) * cos t
 *     Y = (R - x) * sin t
 *     Z = z
 * </pre>
 * and {@link #toStraight(double[])} inverts it. In bent space the straight box becomes a torus sector: an annulus
 * sector in the XY plane, extruded along Z. The bend is valid only when the bend radius is larger than the distance
 * from the inner Y axis to the box face it bends toward, and when the box turns through at most one full circle.
 *
 * <p>{@link #rowIntervals} solves, in closed form, which block indices of one scan row lie inside that sector. The
 * iterator therefore visits no block outside the bent box.
 *
 * <p>Written by Claude Code; conversation transcript: {@code llm/transcripts/transcript_2026-10-09_5135373d-4acd-4a26-a410-3449a9a2abd5.md}.
 */
final class Bend {

    /** Below this value the square of the radial rate of change along a row counts as zero. */
    private static final double FLAT_EPSILON = 1e-18;
    /** Below this magnitude a linear rate of change along a row counts as zero. */
    private static final double ZERO_EPSILON = 1e-12;
    /** Each row interval is widened by this many blocks before rounding, so rounding never drops a boundary block. */
    private static final double K_EPSILON = 1e-7;
    private static final double HALF_PI = Math.PI / 2;
    /**
     * How far past one full circle a turn may go and still count as exactly one circle. Callers compute
     * curvatures with {@code float} constants such as {@code Mth.TWO_PI}, which overshoot {@code 2 * Math.PI} by
     * about {@code 2e-7}; the overshoot only trims the reported inner Y by that much at the ends of the box.
     */
    private static final double FULL_TURN_TOLERANCE = 1e-5;

    /** Length of the scratch array that {@link #rowIntervals} needs. */
    static final int SCRATCH_SIZE = 16;
    /** Maximum number of {@code (lo, hi)} pairs that {@link #rowIntervals} writes. */
    static final int MAX_INTERVALS = 4;

    final double curvature;
    private final double radius;
    /** {@code +1} or {@code -1}: the sign of the curvature. */
    private final double sign;
    private final double midAngle;
    private final double cosMid;
    private final double sinMid;
    /** Half of the angle the box turns through, in {@code (0, PI]}. */
    private final double halfSpan;
    private final double cosHalf;
    private final double sinHalf;
    /** Smallest and largest distance from the bend centre that the straight box reaches. */
    private final double rhoLo;
    private final double rhoHi;
    private final double[] min;
    private final double[] max;

    /**
     * @param curvature non-zero signed curvature in blocks⁻¹
     * @param min       lower corner of the straight box
     * @param max       upper corner of the straight box
     * @throws IllegalArgumentException when the bend radius does not clear the box, or the box turns more than once
     */
    Bend(double curvature, double[] min, double[] max) {
        if (!Double.isFinite(curvature) || curvature == 0) {
            throw new IllegalArgumentException("curvature must be a finite non-zero number, got " + curvature);
        }
        this.curvature = curvature;
        this.radius = 1 / curvature;
        this.sign = Math.signum(curvature);
        this.min = min.clone();
        this.max = max.clone();

        // Distance from the bend centre (R, 0) to a straight point is sign * (R - x); it must stay positive.
        this.rhoLo = sign * (radius - (sign > 0 ? max[0] : min[0]));
        this.rhoHi = sign * (radius - (sign > 0 ? min[0] : max[0]));
        if (!(rhoLo > 0)) {
            throw new IllegalArgumentException("bend radius " + radius + " does not clear the inner box on X in [" + min[0] + ", " + max[0] + ")");
        }
        double span = Math.abs(curvature) * (max[1] - min[1]);
        if (span > 2 * Math.PI + FULL_TURN_TOLERANCE) {
            throw new IllegalArgumentException("inner box turns through " + span + " radians, which is more than one full circle");
        }
        this.halfSpan = Math.min(span / 2, Math.PI);
        this.cosHalf = Math.cos(halfSpan);
        this.sinHalf = Math.sin(halfSpan);
        this.midAngle = curvature * (min[1] + max[1]) / 2;
        this.cosMid = Math.cos(midAngle);
        this.sinMid = Math.sin(midAngle);
    }

    // ---------------------------------------------------------------------------------------------------------
    // Point maps

    /** Converts {@code p} from straight to bent coordinates in place. */
    void toBent(double[] p) {
        double theta = curvature * p[1];
        double arm = radius - p[0];
        p[0] = radius - arm * Math.cos(theta);
        p[1] = arm * Math.sin(theta);
    }

    /** Converts {@code p} from bent to straight coordinates in place. */
    void toStraight(double[] p) {
        double px = sign * (radius - p[0]);
        double py = sign * p[1];
        double rho = Math.sqrt(px * px + py * py);
        // Measure the angle from the middle of the sector so the branch cut of atan2 lies opposite the box.
        double u = px * cosMid + py * sinMid;
        double v = py * cosMid - px * sinMid;
        double phi = Math.atan2(v, u);
        if (sign > 0 && phi == Math.PI) {
            phi = -Math.PI; // a positive curvature covers [-PI, PI) around the middle; a negative one covers (-PI, PI]
        }
        p[0] = radius - sign * rho;
        p[1] = (midAngle + phi) * radius;
    }

    /** {@link #toBent(double[])} for a vector. */
    void toBent(Vector3d p) {
        double[] a = {p.x, p.y, p.z};
        toBent(a);
        p.set(a[0], a[1], a[2]);
    }

    /** {@link #toStraight(double[])} for a vector. */
    void toStraight(Vector3d p) {
        double[] a = {p.x, p.y, p.z};
        toStraight(a);
        p.set(a[0], a[1], a[2]);
    }

    // ---------------------------------------------------------------------------------------------------------
    // Bounds

    /** Writes the bent-space axis-aligned bounding box of the torus sector into {@code outMin} and {@code outMax}. */
    void bentBounds(double[] outMin, double[] outMax) {
        outMin[0] = outMin[1] = Double.POSITIVE_INFINITY;
        outMax[0] = outMax[1] = Double.NEGATIVE_INFINITY;
        double thetaA = Math.min(curvature * min[1], curvature * max[1]);
        double thetaB = Math.max(curvature * min[1], curvature * max[1]);
        // Extremes of the sector lie at its corners or where an arc crosses an axis direction.
        extend(thetaA, outMin, outMax);
        extend(thetaB, outMin, outMax);
        for (long m = (long) Math.ceil(thetaA / HALF_PI); m * HALF_PI <= thetaB; m++) {
            extend(m * HALF_PI, outMin, outMax);
        }
        outMin[2] = min[2];
        outMax[2] = max[2];
    }

    private void extend(double theta, double[] outMin, double[] outMax) {
        double c = Math.cos(theta);
        double s = Math.sin(theta);
        for (double rho : new double[] {rhoLo, rhoHi}) {
            double x = radius - sign * rho * c;
            double y = sign * rho * s;
            outMin[0] = Math.min(outMin[0], x);
            outMax[0] = Math.max(outMax[0], x);
            outMin[1] = Math.min(outMin[1], y);
            outMax[1] = Math.max(outMax[1], y);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Row solving

    /**
     * Finds the runs of block indices {@code k} in {@code [kLo, kHi]} for which the bent point
     * {@code rowStart + k * step} lies inside the annulus sector of this bend (the Z slab is the caller's job).
     *
     * <p>The runs are conservative by a tiny margin, so the caller must still test each block exactly. They are
     * sorted, disjoint and written as {@code (lo, hi)} pairs into {@code out}, which needs room for
     * {@link #MAX_INTERVALS} pairs; {@code scratch} needs {@link #SCRATCH_SIZE} doubles.
     *
     * @return the number of pairs written
     */
    int rowIntervals(double[] rowStart, double[] step, int kLo, int kHi, double[] scratch, int[] out) {
        if (kLo > kHi) {
            return 0;
        }
        // The row relative to the bend centre, in the frame where the arc runs counter-clockwise:
        // p(k) = p0 + k * pd along sign * (R - X), q(k) = q0 + k * qd along sign * Y.
        double p0 = sign * (radius - rowStart[0]);
        double pd = -sign * step[0];
        double q0 = sign * rowStart[1];
        double qd = sign * step[1];

        int annulusCount = annulus(p0, pd, q0, qd, kLo, kHi, scratch, 0);
        if (annulusCount == 0) {
            return 0;
        }
        int wedgeCount = wedge(p0, pd, q0, qd, kLo, kHi, scratch, 4);
        if (wedgeCount == 0) {
            return 0;
        }
        int n = intersect(scratch, 0, annulusCount, 4, wedgeCount, 8);

        int count = 0;
        int previousHi = kLo - 1;
        for (int i = 0; i < n; i++) {
            double lo = Math.max(scratch[8 + 2 * i] - K_EPSILON, previousHi + 1);
            double hi = Math.min(scratch[9 + 2 * i] + K_EPSILON, kHi);
            if (lo <= hi) {
                int kFirst = (int) Math.ceil(lo);
                int kLast = (int) Math.floor(hi);
                if (kFirst <= kLast) {
                    out[2 * count] = kFirst;
                    out[2 * count + 1] = kLast;
                    count++;
                    previousHi = kLast;
                }
            }
        }
        return count;
    }

    /** Runs of {@code k} with {@code rhoLo <= rho(k) <= rhoHi}; {@code rho(k)^2} is a quadratic in {@code k}. */
    private int annulus(double p0, double pd, double q0, double qd, int kLo, int kHi, double[] s, int at) {
        double a = pd * pd + qd * qd;
        double b = 2 * (p0 * pd + q0 * qd);
        double c = p0 * p0 + q0 * q0;
        if (a < FLAT_EPSILON) {
            // The row runs along the bend axis: the distance to the centre is constant, up to a tiny drift.
            double drift = Math.max(Math.abs(kLo), Math.abs(kHi)) * Math.sqrt(a);
            double rho = Math.sqrt(c);
            if (rho + drift < rhoLo || rho - drift > rhoHi) {
                return 0;
            }
            s[at] = kLo;
            s[at + 1] = kHi;
            return 1;
        }
        double[] outer = roots(a, b, c - rhoHi * rhoHi);
        if (outer == null) {
            return 0; // the row never comes within the outer radius
        }
        double lo = Math.max(outer[0], kLo);
        double hi = Math.min(outer[1], kHi);
        if (lo > hi) {
            return 0;
        }
        double[] inner = roots(a, b, c - rhoLo * rhoLo);
        if (inner == null) {
            s[at] = lo;
            s[at + 1] = hi;
            return 1; // the row never dips inside the inner radius
        }
        int n = 0;
        if (lo <= Math.min(hi, inner[0])) {
            s[at] = lo;
            s[at + 1] = Math.min(hi, inner[0]);
            n++;
        }
        if (Math.max(lo, inner[1]) <= hi) {
            s[at + 2 * n] = Math.max(lo, inner[1]);
            s[at + 2 * n + 1] = hi;
            n++;
        }
        return n;
    }

    /** Real roots of {@code a k^2 + b k + c}, ascending, or {@code null} when there are none. */
    private static double @Nullable [] roots(double a, double b, double c) {
        double disc = b * b - 4 * a * c;
        if (disc < 0) {
            return null;
        }
        double q = -0.5 * (b + Math.copySign(Math.sqrt(disc), b));
        double r1 = q / a;
        double r2 = q == 0 ? r1 : c / q;
        return r1 <= r2 ? new double[] {r1, r2} : new double[] {r2, r1};
    }

    /**
     * Runs of {@code k} whose angle around the bend centre lies within {@code halfSpan} of the middle angle. A
     * wedge of at most a half turn is the intersection of two half-planes; a wider one is their union.
     */
    private int wedge(double p0, double pd, double q0, double qd, int kLo, int kHi, double[] s, int at) {
        if (halfSpan >= Math.PI - 1e-12) {
            s[at] = kLo;
            s[at + 1] = kHi;
            return 1; // a full circle has no angular limit
        }
        // Rotate into the frame of the middle angle: u along the middle, v across it.
        double u0 = p0 * cosMid + q0 * sinMid;
        double ud = pd * cosMid + qd * sinMid;
        double v0 = q0 * cosMid - p0 * sinMid;
        double vd = qd * cosMid - pd * sinMid;
        // sin(angle + halfSpan) >= 0 keeps the side past the lower ray; sin(halfSpan - angle) >= 0 the side before the upper ray.
        double[] lower = halfLine(v0 * cosHalf + u0 * sinHalf, vd * cosHalf + ud * sinHalf, kLo, kHi);
        double[] upper = halfLine(u0 * sinHalf - v0 * cosHalf, ud * sinHalf - vd * cosHalf, kLo, kHi);
        if (halfSpan <= HALF_PI) {
            if (lower == null || upper == null) {
                return 0;
            }
            double lo = Math.max(lower[0], upper[0]);
            double hi = Math.min(lower[1], upper[1]);
            if (lo > hi) {
                return 0;
            }
            s[at] = lo;
            s[at + 1] = hi;
            return 1;
        }
        if (lower == null && upper == null) {
            return 0;
        }
        if (lower == null || upper == null) {
            double[] only = lower == null ? upper : lower;
            s[at] = only[0];
            s[at + 1] = only[1];
            return 1;
        }
        double[] first = lower[0] <= upper[0] ? lower : upper;
        double[] second = first == lower ? upper : lower;
        if (second[0] <= first[1]) {
            s[at] = first[0];
            s[at + 1] = Math.max(first[1], second[1]);
            return 1;
        }
        s[at] = first[0];
        s[at + 1] = first[1];
        s[at + 2] = second[0];
        s[at + 3] = second[1];
        return 2;
    }

    /** The part of {@code [kLo, kHi]} where {@code a + b k >= 0}, or {@code null} when it is empty. */
    private static double @Nullable [] halfLine(double a, double b, int kLo, int kHi) {
        double lo = kLo;
        double hi = kHi;
        if (Math.abs(b) < ZERO_EPSILON) {
            if (a < -ZERO_EPSILON) {
                return null;
            }
        } else if (b > 0) {
            lo = Math.max(lo, -a / b);
        } else {
            hi = Math.min(hi, -a / b);
        }
        return lo <= hi ? new double[] {lo, hi} : null;
    }

    /** Intersects two sorted lists of disjoint intervals held in {@code s}; writes the result at {@code outAt}. */
    private static int intersect(double[] s, int aAt, int na, int bAt, int nb, int outAt) {
        int i = 0;
        int j = 0;
        int n = 0;
        while (i < na && j < nb) {
            double lo = Math.max(s[aAt + 2 * i], s[bAt + 2 * j]);
            double hi = Math.min(s[aAt + 2 * i + 1], s[bAt + 2 * j + 1]);
            if (lo <= hi) {
                s[outAt + 2 * n] = lo;
                s[outAt + 2 * n + 1] = hi;
                n++;
            }
            if (s[aAt + 2 * i + 1] < s[bAt + 2 * j + 1]) {
                i++;
            } else {
                j++;
            }
        }
        return n;
    }

    @Override
    public String toString() {
        return "Bend[curvature=" + curvature + ", radius=" + radius + "]";
    }
}
