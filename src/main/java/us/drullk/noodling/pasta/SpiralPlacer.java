package us.drullk.noodling.pasta;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelSimulatedRW;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import tamaized.beanification.Autowired;
import tamaized.beanification.Component;
import us.drullk.noodling.shape.InnerSpaceIterable;
import us.drullk.noodling.shape.InnerSpacePos;
import org.jspecify.annotations.Nullable;
import us.drullk.noodling.noodler.PastaParam;
import us.drullk.noodling.noodler.PastaShape;
import us.drullk.noodling.util.Util;

@Component
public class SpiralPlacer {

    private final Util helper;
    private @Nullable PastaShape shape;

    public SpiralPlacer(@Autowired Util helper) {
        this.helper = helper;
    }

    /**
     * Describes spiral pasta for the Noodler. The thickness slider's upper bound follows the diameter.
     *
     * <p>Written by Claude Code; conversation transcript: {@code llm/transcripts/transcript_2026-10-10_25b84f50-3f88-41a1-a942-98824ffe67dc.md}.
     * <p>Written by Claude Code; conversation transcript: {@code llm/transcripts/transcript_2026-10-10_c3299adc-f781-48a8-9c79-c51c79b26b4b.md}.
     */
    public PastaShape shape() {
        if (this.shape == null) {
            this.shape = PastaShape.builder("spiral")
                    .param(PastaParam.floatSlider("diameter", (random, siblings) -> Mth.lerp(random.nextFloat(), 12, 16), siblings -> new PastaParam.Bounds(12, 16)))
                    .param(PastaParam.floatSlider("height", (random, siblings) -> Mth.lerp(random.nextFloat(), 50, 80), siblings -> new PastaParam.Bounds(50, 80)))
                    .param(PastaParam.floatSlider("thickness", (random, siblings) -> Mth.lerp(Mth.square(random.nextFloat()), 2f, siblings.f("diameter") * 0.25f), siblings -> new PastaParam.Bounds(2f, siblings.f("diameter") * 0.25f)))
                    .param(PastaParam.floatSlider("twist", (random, siblings) -> 1f / Mth.lerp(random.nextFloat(), 3f, 8f), siblings -> new PastaParam.Bounds(1f / 8f, 1f / 3f)))
                    .param(PastaParam.rotation(this.helper))
                    .param(PastaParam.pasta(this.helper))
                    .param(PastaParam.sauce(this.helper))
                    .place((level, center, params) -> this.placeSpiral(level, center, params.rotation(), params.pasta(), params.sauce(), params.f("diameter"), params.f("height"), params.f("thickness"), params.f("twist")));
        }
        return this.shape;
    }

    public void placeSpiral(LevelSimulatedRW level, Vec3 center, Quaterniond rotation, BlockState pasta, BlockState sauce, float diameter, float height, float thickness, float twist) {
        float radius = diameter * 0.5f;
        InnerSpaceIterable spaceIterator = InnerSpaceIterable.centered(center, rotation, diameter, height, diameter);
        for (InnerSpacePos innerSpacePos : spaceIterator) {
            float sampledRadius = Mth.length(innerSpacePos.innerX(), innerSpacePos.innerZ());

            boolean solid = sampledRadius < radius && thickness > Mth.abs(Mth.cos(innerSpacePos.innerY() * twist) * innerSpacePos.innerZ() - Mth.sin(innerSpacePos.innerY() * twist) * innerSpacePos.innerX());
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
