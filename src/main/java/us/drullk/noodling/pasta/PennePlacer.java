package us.drullk.noodling.pasta;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelSimulatedRW;
import net.minecraft.world.level.LevelWriter;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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

public class PennePlacer {

    private final Util helper;
    private @Nullable PastaShape shape;

    public PennePlacer(Util helper) {
        this.helper = helper;
    }

    /**
     * Places a penne with every parameter rolled from {@code random}, through {@link #shape()}.
     *
     * <p>The random calls happen in the same order as before, so this method still gives the same shape for the same
     * random source; the Noodler resolves the same parameters from its own seeds instead.
     *
     * <p>Written by Claude Code; conversation transcript: {@code llm/transcripts/transcript_2026-10-10_25b84f50-3f88-41a1-a942-98824ffe67dc.md}.
     */
    public void placeRandomPenne(LevelSimulatedRW level, Vec3 center, RandomSource random) {
        this.shape().placeRandom(level, center, random);
    }

    /**
     * Describes penne for the Noodler: diameter and height sliders, then the rotation and the pasta block.
     *
     * <p>Written by Claude Code; conversation transcript: {@code llm/transcripts/transcript_2026-10-10_25b84f50-3f88-41a1-a942-98824ffe67dc.md}.
     */
    public PastaShape shape() {
        if (this.shape == null) {
            this.shape = PastaShape.builder("penne")
                    .param(PastaParam.floatSlider("diameter", (random, siblings) -> Mth.lerp(random.nextFloat(), 12, 16), siblings -> new PastaParam.Bounds(12, 16)))
                    .param(PastaParam.floatSlider("height", (random, siblings) -> Mth.lerp(random.nextFloat(), 50, 80), siblings -> new PastaParam.Bounds(50, 80)))
                    .param(PastaParam.rotation(this.helper))
                    .param(PastaParam.pasta(this.helper))
                    .place((level, center, params) -> this.placePenne(level, center, params.rotation(), params.pasta(), Blocks.AIR.defaultBlockState(), params.f("diameter"), params.f("height")));
        }
        return this.shape;
    }

    public void placePenne(LevelSimulatedRW level, Vec3 center, Quaterniond rotation, BlockState pasta, BlockState sauce, float diameter, float height) {
        float radius = diameter * 0.5f;
        InnerSpaceIterable spaceIterator = InnerSpaceIterable.centered(center, rotation, diameter, height, diameter);
        for (InnerSpacePos innerSpacePos : spaceIterator) {
            float sampledRadius = Mth.length(innerSpacePos.innerX(), innerSpacePos.innerZ());

            boolean solid = sampledRadius > radius * 0.667f && sampledRadius < radius // Filter positions for inside and outside the hollow cylinder
                    && 0 > Math.abs(innerSpacePos.innerX() - innerSpacePos.innerY()) * 2 + diameter - height; // Angled cutoffs for the ends
            if (solid) {
                BlockPos pos = innerSpacePos.toBlockPos();
                level.setBlock(pos, pasta, Block.UPDATE_ALL);
                BlockPos above = pos.above();
                if (level.isStateAtPosition(above, BlockBehaviour.BlockStateBase::isAir)) {
                    level.setBlock(above, sauce, Block.UPDATE_ALL);
                }
            }
        }
    }

}
