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

public class PipettePlacer {

    private final Util helper;
    private @Nullable PastaShape shape;

    public PipettePlacer(Util helper) {
        this.helper = helper;
    }

    /**
     * Places a pipette with every parameter rolled from {@code random}, through {@link #shape()}.
     *
     * <p>The random calls happen in the same order as before, so this method still gives the same shape for the same
     * random source; the Noodler resolves the same parameters from its own seeds instead.
     *
     * <p>Written by Claude Code; conversation transcript: {@code llm/transcripts/transcript_2026-10-10_25b84f50-3f88-41a1-a942-98824ffe67dc.md}.
     */
    public void placeRandomPipette(ServerLevel level, Vec3 center, RandomSource random) {
        this.shape().placeRandom(level, center, random);
    }

    /**
     * Describes pipette pasta for the Noodler. Radius and curve are derived fields that follow the diameter and height; only the two pinch radii are random.
     *
     * <p>Written by Claude Code; conversation transcript: {@code llm/transcripts/transcript_2026-10-10_25b84f50-3f88-41a1-a942-98824ffe67dc.md}.
     */
    public PastaShape shape() {
        if (this.shape == null) {
            this.shape = PastaShape.builder("pipette")
                    .param(PastaParam.floatField("diameter", siblings -> 14f))
                    .param(PastaParam.floatField("radius", siblings -> siblings.f("diameter") * 0.5f))
                    .param(PastaParam.floatSlider("pinch_radius_head", (random, siblings) -> Mth.lerp(random.nextFloat(), siblings.f("radius") * 0.5f, siblings.f("radius")), siblings -> new PastaParam.Bounds(siblings.f("radius") * 0.5f, siblings.f("radius"))))
                    .param(PastaParam.floatSlider("pinch_radius_tail", (random, siblings) -> Mth.lerp(random.nextFloat(), siblings.f("radius") * 0.5f, siblings.f("radius")), siblings -> new PastaParam.Bounds(siblings.f("radius") * 0.5f, siblings.f("radius"))))
                    .param(PastaParam.floatField("height", siblings -> 25f))
                    .param(PastaParam.floatField("thickness", siblings -> 3.125f))
                    .param(PastaParam.floatField("curve", siblings -> siblings.f("height") * 0.75f))
                    .param(PastaParam.rotation(this.helper))
                    .param(PastaParam.pasta(this.helper))
                    .place((level, center, params) -> this.placePipette(level, center, params.rotation(), params.pasta(),
                            params.f("diameter"), params.f("height"), params.f("curve"), params.f("radius"), params.f("pinch_radius_head"), params.f("pinch_radius_tail"), params.f("thickness")));
        }
        return this.shape;
    }

    public void placePipette(ServerLevel level, Vec3 center, Quaterniond rotation, BlockState pasta, float diameter, float height, float curve, float radius, float pinchRadiusHead, float pinchRadiusTail, float thickness) {
        InnerSpaceIterable spaceIterator = InnerSpaceIterable.centered(center, rotation, diameter, height, diameter);
        for (InnerSpacePos innerSpacePos : spaceIterator.bentByRadius(curve)) {
            // Percentile of arc progression along Y-axis
            float arcLerpFactor = innerSpacePos.innerY() / height + 0.5f;

            float pinchLerp = Mth.square(Mth.square(arcLerpFactor * 2f - 1f)); // Rests at 0 when at Y-axis center. Quadratically approaches 1 when Y-pos approaches either end
            float pinchRadius = Mth.lerp(pinchLerp, radius, Mth.lerp(arcLerpFactor, pinchRadiusHead, pinchRadiusTail));
            float sampledRadius = Mth.length(innerSpacePos.innerX() * radius / pinchRadius, innerSpacePos.innerZ());

            boolean solid = sampledRadius > radius - thickness && sampledRadius < radius;
            if (solid) level.setBlock(innerSpacePos.toBlockPos(), pasta, Block.UPDATE_ALL);
        }
    }

}
