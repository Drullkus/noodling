package us.drullk.noodling.noodler;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.AxisAngle4d;
import org.joml.Quaterniond;

/**
 * One value that a pasta shape parameter can hold.
 *
 * <p>Numbers cover both float sliders and integer sliders (an integer is stored as a whole double). A block value
 * holds the pasta block state, or the sauce block state (air for no sauce). A rotation holds the axis-angle pair that the Noodler GUI shows; it converts to the
 * {@link Quaterniond} that the placers take.
 *
 * <p>Written by Claude Code; conversation transcript: {@code llm/transcripts/transcript_2026-10-10_25b84f50-3f88-41a1-a942-98824ffe67dc.md}.
 * <p>Written by Claude Code; conversation transcript: {@code llm/transcripts/transcript_2026-10-10_c3299adc-f781-48a8-9c79-c51c79b26b4b.md}.
 */
public sealed interface ParamValue permits ParamValue.Num, ParamValue.Block, ParamValue.Rotation {

    Codec<ParamValue> CODEC = Codec.STRING.dispatch(ParamValue::type, type -> switch (type) {
        case Num.TYPE -> Num.MAP_CODEC;
        case Block.TYPE -> Block.MAP_CODEC;
        case Rotation.TYPE -> Rotation.MAP_CODEC;
        default -> throw new IllegalArgumentException("Unknown parameter value type " + type);
    });

    /** The discriminator that {@link #CODEC} writes. */
    String type();

    /** A float or integer parameter. */
    record Num(double value) implements ParamValue {
        static final String TYPE = "number";
        static final MapCodec<Num> MAP_CODEC = Codec.DOUBLE.fieldOf("value").xmap(Num::new, Num::value);

        public static Num of(float value) {
            return new Num(value);
        }

        public static Num of(int value) {
            return new Num(value);
        }

        public float asFloat() {
            return (float) this.value;
        }

        public int asInt() {
            return (int) Math.round(this.value);
        }

        @Override
        public String type() {
            return TYPE;
        }
    }

    /** A pasta block state, or a sauce block state where air means no sauce. */
    record Block(BlockState state) implements ParamValue {
        static final String TYPE = "block";
        static final MapCodec<Block> MAP_CODEC = BlockState.CODEC.fieldOf("state").xmap(Block::new, Block::state);

        @Override
        public String type() {
            return TYPE;
        }
    }

    /**
     * An axis-angle rotation. The axis does not need to be normalized; a zero axis means no rotation. Negating the
     * axis turns the rotation the other way, which is why the GUI only needs one angle field.
     */
    record Rotation(Vec3 axis, float angle) implements ParamValue {
        static final String TYPE = "rotation";
        static final MapCodec<Rotation> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Vec3.CODEC.fieldOf("axis").forGetter(Rotation::axis),
                Codec.FLOAT.fieldOf("angle").forGetter(Rotation::angle)
        ).apply(instance, Rotation::new));

        public static final Rotation IDENTITY = new Rotation(new Vec3(0, 1, 0), 0);

        /** Converts a quaternion to the axis-angle pair the GUI shows. */
        public static Rotation of(Quaterniond rotation) {
            AxisAngle4d axisAngle = new AxisAngle4d(rotation);
            return new Rotation(new Vec3(axisAngle.x, axisAngle.y, axisAngle.z), (float) axisAngle.angle);
        }

        /** Converts back to the quaternion the placers take. */
        public Quaterniond toQuaternion() {
            double length = this.axis.length();
            if (!(length > 1.0e-9) || !Double.isFinite(length)) {
                return new Quaterniond();
            }
            Vec3 unit = this.axis.scale(1.0 / length);
            return new Quaterniond().rotationAxis(this.angle, unit.x, unit.y, unit.z);
        }

        @Override
        public String type() {
            return TYPE;
        }
    }
}
