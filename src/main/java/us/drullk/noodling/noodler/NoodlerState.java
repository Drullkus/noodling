package us.drullk.noodling.noodler;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Map;
import java.util.Optional;

/**
 * Everything the Noodler GUI edits, stored in one data component on the item.
 *
 * <p>{@code seed} is free text parsed like a world seed. When {@code seedLocked} is false the Noodler rolls a new
 * seed on every placement and writes it here, so the player can lock the seed that made a shape they like. An empty
 * {@code shape} means "random pasta". {@code params} holds the per-parameter state for the selected shape, keyed by
 * parameter name; it is cleared when the shape changes.
 *
 * <p>Written by Claude Code; conversation transcript: {@code llm/transcripts/transcript_2026-10-10_25b84f50-3f88-41a1-a942-98824ffe67dc.md}.
 */
public record NoodlerState(String seed, boolean seedLocked, Optional<String> shape, Map<String, ParamState> params) {

    public static final Codec<NoodlerState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("seed", "").forGetter(NoodlerState::seed),
            Codec.BOOL.optionalFieldOf("seed_locked", false).forGetter(NoodlerState::seedLocked),
            Codec.STRING.optionalFieldOf("shape").forGetter(NoodlerState::shape),
            Codec.unboundedMap(Codec.STRING, ParamState.CODEC).optionalFieldOf("params", Map.of()).forGetter(NoodlerState::params)
    ).apply(instance, NoodlerState::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, NoodlerState> STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(CODEC);

    public static final NoodlerState DEFAULT = new NoodlerState("", false, Optional.empty(), Map.of());

    public NoodlerState {
        params = Map.copyOf(params);
    }

    public ParamState param(String name) {
        return this.params.getOrDefault(name, ParamState.EMPTY);
    }

    public NoodlerState withSeed(String seed) {
        return new NoodlerState(seed, this.seedLocked, this.shape, this.params);
    }

    public NoodlerState withParams(Map<String, ParamState> params) {
        return new NoodlerState(this.seed, this.seedLocked, this.shape, params);
    }
}
