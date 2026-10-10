package us.drullk.noodling.pasta;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import us.drullk.noodling.shape.InnerSpaceIterable;
import us.drullk.noodling.shape.InnerSpacePos;
import org.jspecify.annotations.Nullable;
import us.drullk.noodling.noodler.PastaParam;
import us.drullk.noodling.noodler.PastaShape;
import us.drullk.noodling.util.Util;

public class ShellPlacer {

    private final Util helper;
    private @Nullable PastaShape shape;

    public ShellPlacer(Util helper) {
        this.helper = helper;
    }

    /**
     * Places a shell with every parameter rolled from {@code random}, through {@link #shape()}.
     *
     * <p>The random calls happen in the same order as before, so this method still gives the same shape for the same
     * random source; the Noodler resolves the same parameters from its own seeds instead.
     *
     * <p>Written by Claude Code; conversation transcript: {@code llm/transcripts/transcript_2026-10-10_25b84f50-3f88-41a1-a942-98824ffe67dc.md}.
     */
    public void placeRandomShell(ServerLevel level, Vec3 center, RandomSource random) {
        this.shape().placeRandom(level, center, random);
    }

    /**
     * Describes shells for the Noodler. The height slider's bounds follow the diameter, matching the triangle roll.
     *
     * <p>Written by Claude Code; conversation transcript: {@code llm/transcripts/transcript_2026-10-10_25b84f50-3f88-41a1-a942-98824ffe67dc.md}.
     */
    public PastaShape shape() {
        if (this.shape == null) {
            this.shape = PastaShape.builder("shell")
                    .param(PastaParam.floatSlider("diameter", (random, siblings) -> Mth.lerp(random.nextFloat(), 12, 16), siblings -> new PastaParam.Bounds(12, 16)))
                    .param(PastaParam.floatField("thickness", siblings -> 2.5f))
                    .param(PastaParam.floatSlider("height", (random, siblings) -> siblings.f("diameter") * random.triangle(1.25f, 0.2f), siblings -> new PastaParam.Bounds(siblings.f("diameter") * 1.05f, siblings.f("diameter") * 1.45f)))
                    .param(PastaParam.floatSlider("taper_factor", (random, siblings) -> Mth.lerp(random.nextFloat(), 2, 3), siblings -> new PastaParam.Bounds(2, 3)))
                    .param(PastaParam.rotation(this.helper))
                    .param(PastaParam.pasta(this.helper))
                    .place((level, center, params) -> this.placeShell(level, center, params.rotation(), params.pasta(), params.f("height"), params.f("diameter"), params.f("taper_factor"), params.f("thickness")));
        }
        return this.shape;
    }

    public void placeShell(ServerLevel level, Vec3 center, Quaterniond rotation, BlockState pasta, float height, float diameter, float taperFactor, float thickness) {
        float halfHeight = height * 0.65f; // Slightly over half, or else orphaned voxels (blocks) will place
        float radius = diameter * 0.5f;
        InnerSpaceIterable spaceIterator = InnerSpaceIterable.centered(center, rotation, diameter, height, diameter);
        for (InnerSpacePos innerSpacePos : spaceIterator) {
            double sampledRadian = Mth.atan2(innerSpacePos.innerZ(), innerSpacePos.innerX()) + Mth.PI;
            float sampledRadius = Mth.length(innerSpacePos.innerX(), innerSpacePos.innerZ());
            double taper = Math.pow(1 - Mth.square(innerSpacePos.innerY() / halfHeight), taperFactor);

            double radiusThreshold = Math.sqrt(sampledRadian / Mth.TWO_PI) * radius * taper; // Offset of shell manifold from inner-space's Y axis
            boolean solid = sampledRadius < radiusThreshold && sampledRadius > radiusThreshold - thickness;
            if (solid) level.setBlock(innerSpacePos.toBlockPos(), pasta, Block.UPDATE_ALL);
        }
    }

}
