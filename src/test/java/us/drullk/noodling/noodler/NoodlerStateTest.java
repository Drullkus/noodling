package us.drullk.noodling.noodler;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.google.gson.JsonElement;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round-trips the Noodler's data component through its codec, covering every parameter value type and every
 * parameter state (unlocked, locked with seed, locked manual).
 *
 * <p>Written by Claude Code; conversation transcript: {@code llm/transcripts/transcript_2026-10-10_25b84f50-3f88-41a1-a942-98824ffe67dc.md}.
 */
class NoodlerStateTest {

    private static NoodlerState sample() {
        return new NoodlerState("banana", true, Optional.of("shell"), Map.of(
                "diameter", new ParamState("77", true, Optional.of(ParamValue.Num.of(13.5F))),
                "fins", new ParamState("", true, Optional.of(ParamValue.Num.of(4))),
                "thickness", ParamState.EMPTY,
                PastaParam.PASTA, new ParamState("", false, Optional.of(new ParamValue.Block(Blocks.STONE.defaultBlockState()))),
                PastaParam.ROTATION, new ParamState("9", true, Optional.of(new ParamValue.Rotation(new Vec3(0.5, -1, 2), 1.25F)))
        ));
    }

    @Test
    void codecRoundTrip() {
        NoodlerState state = sample();
        DataResult<JsonElement> encoded = NoodlerState.CODEC.encodeStart(JsonOps.INSTANCE, state);
        JsonElement json = encoded.getOrThrow();
        NoodlerState decoded = NoodlerState.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        assertEquals(state, decoded);
    }

    @Test
    void defaultStateDecodesFromEmptyObject() {
        NoodlerState decoded = NoodlerState.CODEC.parse(JsonOps.INSTANCE, new com.google.gson.JsonObject()).getOrThrow();
        assertEquals(NoodlerState.DEFAULT, decoded);
    }

    @Test
    void manualStateMeansLockedWithoutSeed() {
        assertTrue(new ParamState("", true, Optional.empty()).isManual());
        assertTrue(!new ParamState("5", true, Optional.empty()).isManual());
        assertTrue(!ParamState.EMPTY.isManual());
    }

    @Test
    void rotationRoundTripsThroughQuaternion() {
        ParamValue.Rotation rotation = new ParamValue.Rotation(new Vec3(0, 0, 3), 0.75F);
        ParamValue.Rotation back = ParamValue.Rotation.of(rotation.toQuaternion());
        assertEquals(0.75, back.angle(), 1.0e-5);
        assertEquals(1.0, back.axis().z, 1.0e-9);
        assertEquals(0.0, back.axis().x, 1.0e-9);
    }

    @Test
    void zeroAxisIsIdentity() {
        assertEquals(1.0, new ParamValue.Rotation(Vec3.ZERO, 2.0F).toQuaternion().w, 1.0e-12);
    }
}
