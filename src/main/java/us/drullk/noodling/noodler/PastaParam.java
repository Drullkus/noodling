package us.drullk.noodling.noodler;

import net.minecraft.util.RandomSource;
import org.jspecify.annotations.Nullable;
import us.drullk.noodling.util.Util;

/**
 * One parameter of a pasta shape: its name, how the GUI edits it, how a {@code placeRandom*} call rolls it, and the
 * bounds of its slider.
 *
 * <p>Rolls and bounds receive the parameters declared before this one, so a bound can depend on a sibling (for
 * example a shell's height depends on its diameter). Constants and derived values are fields whose roll does not
 * touch the random source.
 *
 * <p>Written by Claude Code; conversation transcript: {@code llm/transcripts/transcript_2026-10-10_25b84f50-3f88-41a1-a942-98824ffe67dc.md}.
 * <p>Written by Claude Code; conversation transcript: {@code llm/transcripts/transcript_2026-10-10_c3299adc-f781-48a8-9c79-c51c79b26b4b.md}.
 */
public final class PastaParam {

    /** Name of the rotation parameter that every shape declares. */
    public static final String ROTATION = "rotation";
    /** Name of the pasta block parameter that every shape declares. */
    public static final String PASTA = "pasta";
    /** Name of the sauce block parameter that every shape declares. */
    public static final String SAUCE = "sauce";

    /** How the GUI edits the parameter. */
    public enum Kind {
        FLOAT_SLIDER,
        INT_SLIDER,
        FLOAT_FIELD,
        BLOCK,
        /** A sauce block, or air for no sauce; the GUI shows a dropdown with a "no sauce" entry. */
        SAUCE,
        ROTATION
    }

    /** Inclusive slider bounds. */
    public record Bounds(float min, float max) {
        public float clamp(float value) {
            return Math.max(this.min, Math.min(this.max, value));
        }

        public int clamp(int value) {
            return Math.max(Math.round(this.min), Math.min(Math.round(this.max), value));
        }
    }

    @FunctionalInterface
    public interface Roll {
        ParamValue roll(RandomSource random, ResolvedParams siblings);
    }

    @FunctionalInterface
    public interface FloatRoll {
        float roll(RandomSource random, ResolvedParams siblings);
    }

    @FunctionalInterface
    public interface IntRoll {
        int roll(RandomSource random, ResolvedParams siblings);
    }

    @FunctionalInterface
    public interface Derived {
        float get(ResolvedParams siblings);
    }

    @FunctionalInterface
    public interface BoundsFunction {
        Bounds bounds(ResolvedParams siblings);
    }

    private final String name;
    private final Kind kind;
    private final Roll roll;
    private final @Nullable BoundsFunction bounds;

    private PastaParam(String name, Kind kind, Roll roll, @Nullable BoundsFunction bounds) {
        this.name = name;
        this.kind = kind;
        this.roll = roll;
        this.bounds = bounds;
    }

    /** A float with a random distribution and a slider. */
    public static PastaParam floatSlider(String name, FloatRoll roll, BoundsFunction bounds) {
        return new PastaParam(name, Kind.FLOAT_SLIDER, (random, siblings) -> ParamValue.Num.of(roll.roll(random, siblings)), bounds);
    }

    /** An integer with a random distribution and a slider. */
    public static PastaParam intSlider(String name, IntRoll roll, BoundsFunction bounds) {
        return new PastaParam(name, Kind.INT_SLIDER, (random, siblings) -> ParamValue.Num.of(roll.roll(random, siblings)), bounds);
    }

    /** A constant or a value derived from earlier parameters; the GUI shows a number field. */
    public static PastaParam floatField(String name, Derived value) {
        return new PastaParam(name, Kind.FLOAT_FIELD, (random, siblings) -> ParamValue.Num.of(value.get(siblings)), null);
    }

    /** The rotation of the inner space, rolled by {@link Util#nextRotation(RandomSource)}. */
    public static PastaParam rotation(Util helper) {
        return new PastaParam(ROTATION, Kind.ROTATION, (random, siblings) -> ParamValue.Rotation.of(helper.nextRotation(random)), null);
    }

    /** The pasta block, rolled by {@link Util#getRandomPastaBlock(RandomSource)}. */
    public static PastaParam pasta(Util helper) {
        return new PastaParam(PASTA, Kind.BLOCK, (random, siblings) -> new ParamValue.Block(helper.getRandomPastaBlock(random)), null);
    }

    /** The sauce block (air for no sauce), rolled by {@link Util#getRandomSauceBlock(RandomSource)}. */
    public static PastaParam sauce(Util helper) {
        return new PastaParam(SAUCE, Kind.SAUCE, (random, siblings) -> new ParamValue.Block(helper.getRandomSauceBlock(random)), null);
    }

    public String name() {
        return this.name;
    }

    public Kind kind() {
        return this.kind;
    }

    /** Translation key of the label the GUI shows. */
    public String descriptionId() {
        return "noodling.param." + this.name;
    }

    public ParamValue roll(RandomSource random, ResolvedParams siblings) {
        return this.roll.roll(random, siblings);
    }

    /** Slider bounds for the current sibling values; null for fields, blocks and rotations. */
    public @Nullable Bounds bounds(ResolvedParams siblings) {
        return this.bounds == null ? null : this.bounds.bounds(siblings);
    }

    /** True when the value has the type this parameter expects. */
    public boolean accepts(ParamValue value) {
        return switch (this.kind) {
            case FLOAT_SLIDER, INT_SLIDER, FLOAT_FIELD -> value instanceof ParamValue.Num;
            case BLOCK, SAUCE -> value instanceof ParamValue.Block;
            case ROTATION -> value instanceof ParamValue.Rotation;
        };
    }
}
