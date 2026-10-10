package us.drullk.noodling.pasta;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelSimulatedRW;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import us.drullk.noodling.shape.InnerSpaceIterable;
import us.drullk.noodling.shape.InnerSpacePos;
import org.jspecify.annotations.Nullable;
import us.drullk.noodling.noodler.PastaParam;
import us.drullk.noodling.noodler.PastaShape;
import us.drullk.noodling.util.Util;

public class RadiatoriPlacer {

    private final Util helper;
    private @Nullable PastaShape shape;

    public RadiatoriPlacer(Util helper) {
        this.helper = helper;
    }

    /**
     * Places a radiatori with every parameter rolled from {@code random}, through {@link #shape()}.
     *
     * <p>The random calls happen in the same order as before, so this method still gives the same shape for the same
     * random source; the Noodler resolves the same parameters from its own seeds instead.
     *
     * <p>Written by Claude Code; conversation transcript: {@code llm/transcripts/transcript_2026-10-10_25b84f50-3f88-41a1-a942-98824ffe67dc.md}.
     */
    public void placeRandomRadiatori(LevelSimulatedRW level, Vec3 center, RandomSource random) {
        this.shape().placeRandom(level, center, random);
    }

    /**
     * Describes radiatori for the Noodler, keeping the roll order of the hand-written version: diameters, fin count, fin height, frilling, cutoff.
     *
     * <p>Written by Claude Code; conversation transcript: {@code llm/transcripts/transcript_2026-10-10_25b84f50-3f88-41a1-a942-98824ffe67dc.md}.
     * <p>Written by Claude Code; conversation transcript: {@code llm/transcripts/transcript_2026-10-10_c3299adc-f781-48a8-9c79-c51c79b26b4b.md}.
     */
    public PastaShape shape() {
        if (this.shape == null) {
            this.shape = PastaShape.builder("radiatori")
                    .param(PastaParam.floatSlider("diameter_head", (random, siblings) -> Mth.lerp(random.nextFloat(), 6, 12), siblings -> new PastaParam.Bounds(6, 12)))
                    .param(PastaParam.floatSlider("diameter_tail", (random, siblings) -> Mth.lerp(random.nextFloat(), 6, 12), siblings -> new PastaParam.Bounds(6, 12)))
                    .param(PastaParam.floatField("core_thickness", siblings -> 2.5f))
                    .param(PastaParam.intSlider("fins", (random, siblings) -> random.nextIntBetweenInclusive(3, 6), siblings -> new PastaParam.Bounds(3, 6)))
                    .param(PastaParam.floatField("fin_width", siblings -> 3f))
                    .param(PastaParam.floatField("fin_spacing", siblings -> 3.5f))
                    .param(PastaParam.floatSlider("fin_height", (random, siblings) -> Mth.lerp(random.nextFloat(), 4, 6), siblings -> new PastaParam.Bounds(4, 6)))
                    .param(PastaParam.floatSlider("fin_frilling_freq_head", (random, siblings) -> Mth.lerp(Mth.square(random.nextFloat()), 6, 2), siblings -> new PastaParam.Bounds(2, 6)))
                    .param(PastaParam.floatSlider("fin_frilling_freq_tail", (random, siblings) -> Mth.lerp(Mth.square(random.nextFloat()), 6, 2), siblings -> new PastaParam.Bounds(2, 6)))
                    .param(PastaParam.floatSlider("fin_frilling_amp_head", (random, siblings) -> Mth.lerp(Mth.square(random.nextFloat()), 1.5f, 0), siblings -> new PastaParam.Bounds(0, 1.5f)))
                    .param(PastaParam.floatSlider("fin_frilling_amp_tail", (random, siblings) -> Mth.lerp(Mth.square(random.nextFloat()), 1.5f, 0), siblings -> new PastaParam.Bounds(0, 1.5f)))
                    .param(PastaParam.floatSlider("cutoff_radian", (random, siblings) -> Mth.lerp(Mth.square(random.nextFloat()), 0.9f, 2f/3f) * Mth.TWO_PI, siblings -> new PastaParam.Bounds(2f/3f * Mth.TWO_PI, 0.9f * Mth.TWO_PI)))
                    .param(PastaParam.rotation(this.helper))
                    .param(PastaParam.pasta(this.helper))
                    .param(PastaParam.sauce(this.helper))
                    .place((level, center, params) -> this.placeRadiatori(level, center, params.rotation(), params.pasta(), params.sauce(),
                            params.f("diameter_head"), params.f("diameter_tail"), params.f("fin_height"), params.f("fin_width"), params.f("fin_spacing"), params.i("fins"), params.f("cutoff_radian"),
                            params.f("fin_frilling_freq_head"), params.f("fin_frilling_freq_tail"), params.f("fin_frilling_amp_head"), params.f("fin_frilling_amp_tail"), params.f("core_thickness")));
        }
        return this.shape;
    }

    public void placeRadiatori(LevelSimulatedRW level, Vec3 center, Quaterniond rotation, BlockState pasta, BlockState sauce, float diameterHead, float diameterTail, float finHeight, float finWidth, float finSpacing, int fins, float cutoffRadian, float finFrillingFreqHead, float finFrillingFreqTail, float finFrillingAmpHead, float finFrillingAmpTail, float coreThickness) {
        float radiusHead = diameterHead * 0.5f, radiusTail = diameterTail * 0.5f;
        float diameterMax = Math.max(diameterHead, diameterTail) + finHeight * 2f;
        float finUnit = finWidth + finSpacing;
        float length = fins * finUnit + finSpacing; // Total length of radiatori from fin count, plus extra spacer.
        float lengthHalf = length * 0.5f;
        InnerSpaceIterable spaceIterator = InnerSpaceIterable.centered(center, rotation, diameterMax, length, diameterMax);
        for (InnerSpacePos innerSpacePos : spaceIterator) {
            // Clockwise angle around Y-Axis of the inner space, ranging [0, 2PI)
            double sampledRadian = Mth.atan2(innerSpacePos.innerZ(), -innerSpacePos.innerX()) + Mth.PI;
            if (sampledRadian > cutoffRadian) continue;

            float sampledRadius = Mth.length(innerSpacePos.innerX(), innerSpacePos.innerZ());

            // Percentile of arc progression, clockwise from X-axis positive towards Z-axis negative.
            double arcLerpFactor = sampledRadian / cutoffRadian;

            // Radius at given point of arc around Y-axis (for the pasta's slight curling)
            double radiusThreshold = Mth.lerp(arcLerpFactor, radiusHead, radiusTail);

            // Y-offset to produce the frilling in the fin checks.
            double frillingYOffset = Mth.cos(sampledRadian * Mth.lerp(arcLerpFactor, finFrillingFreqHead, finFrillingFreqTail)) // The wave for the frills.
                    * Mth.lerp(arcLerpFactor, finFrillingAmpHead, finFrillingAmpTail) // Amplitude of frilling, in respect to arc progression.
                    * Mth.inverseLerp(sampledRadius, radiusThreshold, radiusThreshold + finHeight); // Flatten frills towards the core, keeping full frilling at the extremity.

            double yAdjusted = frillingYOffset + innerSpacePos.innerY() + lengthHalf; // Adjusted y pos for checking if within frill in pasta shape.
            int fin = Mth.floor(yAdjusted / finUnit); // Which fin, from the bottom, that the Y-pos samples. In respect to the frilling.
            boolean isYWithinFin = (yAdjusted % finUnit > finSpacing) // True if the adjusted Y-pos outside the fin gap, moduled per fin unit.
                    && fin >= 0 && fin < fins; // Avoid extra fins/stray voxels via rounding error.

            boolean solid = sampledRadius > radiusThreshold - coreThickness && sampledRadius < radiusThreshold + (isYWithinFin ? finHeight : 0f);
            if (solid) {
                BlockPos pos = innerSpacePos.toBlockPos();
                level.setBlock(pos, pasta, Block.UPDATE_ALL);
                BlockPos above = pos.above();
                if (!sauce.isAir() && level.isStateAtPosition(above, BlockBehaviour.BlockStateBase::isAir)) {
                    level.setBlock(above, sauce, Block.UPDATE_ALL);
                }
            }
        }
    }

}
