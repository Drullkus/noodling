package us.drullk.noodling.noodler;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;

/**
 * The GUI state of one pasta shape parameter on a Noodler.
 *
 * <p>The seed is free text that {@link NoodlerSeeds#parse(String)} reads the same way the world creation screen
 * does; an empty string means "no seed". The three states are:
 * <ul>
 *     <li>unlocked: the value is rolled from a seed derived from the Noodler's seed on every placement, and the
 *     rolled seed and value are written back so the knob shows them;</li>
 *     <li>locked with a seed: the value is rolled from that seed, so it stays the same on every placement;</li>
 *     <li>locked without a seed: the value is used as typed. Editing the knob clears the seed to reach this state.</li>
 * </ul>
 *
 * <p>Written by Claude Code; conversation transcript: {@code llm/transcripts/transcript_2026-10-10_25b84f50-3f88-41a1-a942-98824ffe67dc.md}.
 */
public record ParamState(String seed, boolean locked, Optional<ParamValue> value) {

    public static final Codec<ParamState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("seed", "").forGetter(ParamState::seed),
            Codec.BOOL.optionalFieldOf("locked", false).forGetter(ParamState::locked),
            ParamValue.CODEC.optionalFieldOf("value").forGetter(ParamState::value)
    ).apply(instance, ParamState::new));

    public static final ParamState EMPTY = new ParamState("", false, Optional.empty());

    public ParamState withSeed(String seed) {
        return new ParamState(seed, this.locked, this.value);
    }

    public ParamState withLocked(boolean locked) {
        return new ParamState(this.seed, locked, this.value);
    }

    public ParamState withValue(ParamValue value) {
        return new ParamState(this.seed, this.locked, Optional.of(value));
    }

    /** True when the value is used as typed: locked and without a seed. */
    public boolean isManual() {
        return this.locked && this.seed.isBlank();
    }
}
