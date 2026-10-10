package us.drullk.noodling.noodler;

import net.minecraft.world.level.block.state.BlockState;
import org.joml.Quaterniond;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The values of a shape's parameters in declaration order, with typed accessors for the placers.
 *
 * <p>Parameters are added one at a time while a shape is resolved, so a parameter's roll or bounds can read the
 * parameters declared before it.
 *
 * <p>Written by Claude Code; conversation transcript: {@code llm/transcripts/transcript_2026-10-10_25b84f50-3f88-41a1-a942-98824ffe67dc.md}.
 * <p>Written by Claude Code; conversation transcript: {@code llm/transcripts/transcript_2026-10-10_c3299adc-f781-48a8-9c79-c51c79b26b4b.md}.
 */
public final class ResolvedParams {

    private final Map<String, ParamValue> values = new LinkedHashMap<>();

    public void put(String name, ParamValue value) {
        this.values.put(name, value);
    }

    public ParamValue get(String name) {
        ParamValue value = this.values.get(name);
        if (value == null) {
            throw new IllegalArgumentException("Parameter " + name + " is not resolved yet");
        }
        return value;
    }

    public boolean has(String name) {
        return this.values.containsKey(name);
    }

    public Map<String, ParamValue> values() {
        return Collections.unmodifiableMap(this.values);
    }

    public float f(String name) {
        return ((ParamValue.Num) this.get(name)).asFloat();
    }

    public int i(String name) {
        return ((ParamValue.Num) this.get(name)).asInt();
    }

    public BlockState block(String name) {
        return ((ParamValue.Block) this.get(name)).state();
    }

    public Quaterniond rotation(String name) {
        return ((ParamValue.Rotation) this.get(name)).toQuaternion();
    }

    /** The rotation parameter every shape declares. */
    public Quaterniond rotation() {
        return this.rotation(PastaParam.ROTATION);
    }

    /** The pasta block parameter every shape declares. */
    public BlockState pasta() {
        return this.block(PastaParam.PASTA);
    }

    /** The sauce block parameter every shape declares; air means no sauce. */
    public BlockState sauce() {
        return this.block(PastaParam.SAUCE);
    }
}
