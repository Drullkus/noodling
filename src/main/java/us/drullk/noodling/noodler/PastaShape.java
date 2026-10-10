package us.drullk.noodling.noodler;

import net.minecraft.world.level.LevelWriter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Describes a pasta shape for the Noodler: an ordered list of {@link PastaParam}s and the placement call that takes
 * their resolved values.
 *
 * <p>{@link #placeRandom} rolls every parameter in order from one random source, which gives the same results as
 * the hand-written {@code placeRandom*} methods did; the Noodler resolves each parameter from its own seed instead.
 * Placement writes through {@link LevelWriter}, so a shape can go into a world or into a
 * {@link RecordingLevelWriter} for export.
 *
 * <p>Written by Claude Code; conversation transcript: {@code llm/transcripts/transcript_2026-10-10_25b84f50-3f88-41a1-a942-98824ffe67dc.md}.
 */
public final class PastaShape {

    @FunctionalInterface
    public interface Placement {
        void place(LevelWriter level, Vec3 center, ResolvedParams params);
    }

    private final String id;
    private final List<PastaParam> params;
    private final Placement placement;

    private PastaShape(String id, List<PastaParam> params, Placement placement) {
        this.id = id;
        this.params = List.copyOf(params);
        this.placement = placement;
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public String id() {
        return this.id;
    }

    /** Translation key of the name the GUI shows. */
    public String descriptionId() {
        return "noodling.shape." + this.id;
    }

    public List<PastaParam> params() {
        return this.params;
    }

    public @Nullable PastaParam param(String name) {
        for (PastaParam param : this.params) {
            if (param.name().equals(name)) {
                return param;
            }
        }
        return null;
    }

    /** Rolls every parameter in order from {@code random}. */
    public ResolvedParams resolveAll(RandomSource random) {
        ResolvedParams values = new ResolvedParams();
        for (PastaParam param : this.params) {
            values.put(param.name(), param.roll(random, values));
        }
        return values;
    }

    public void place(LevelWriter level, Vec3 center, ResolvedParams params) {
        this.placement.place(level, center, params);
    }

    public void placeRandom(LevelWriter level, Vec3 center, RandomSource random) {
        this.place(level, center, this.resolveAll(random));
    }

    public static final class Builder {
        private final String id;
        private final List<PastaParam> params = new ArrayList<>();

        private Builder(String id) {
            this.id = id;
        }

        public Builder param(PastaParam param) {
            if (this.params.stream().anyMatch(existing -> existing.name().equals(param.name()))) {
                throw new IllegalArgumentException("Duplicate parameter " + param.name() + " on shape " + this.id);
            }
            this.params.add(param);
            return this;
        }

        public PastaShape place(Placement placement) {
            return new PastaShape(this.id, this.params, placement);
        }
    }
}
