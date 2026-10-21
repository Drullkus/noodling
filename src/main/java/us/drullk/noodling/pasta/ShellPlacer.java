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
import tamaized.beanification.Bean;
import tamaized.beanification.Component;
import us.drullk.noodling.shape.InnerSpaceIterable;
import us.drullk.noodling.shape.InnerSpacePos;
import us.drullk.noodling.noodler.PastaParam;
import us.drullk.noodling.noodler.PastaShape;
import us.drullk.noodling.util.Util;

@Component
public class ShellPlacer {

    /**
     * Describes shells for the Noodler. The height slider's bounds follow the diameter, matching the triangle roll.
     *
     * <p>Written by Claude Code; conversation transcript: {@code llm/transcripts/transcript_2026-10-10_25b84f50-3f88-41a1-a942-98824ffe67dc.md}.
     * <p>Written by Claude Code; conversation transcript: {@code llm/transcripts/transcript_2026-10-10_c3299adc-f781-48a8-9c79-c51c79b26b4b.md}.
     */
    @Bean("shellShape")
    public static PastaShape shape(@Autowired ShellPlacer shellPlacer, @Autowired Util helper) {
        return PastaShape.builder("shell")
                .param(PastaParam.floatSlider("diameter", (random, siblings) -> Mth.lerp(random.nextFloat(), 12, 16), siblings -> new PastaParam.Bounds(12, 16)))
                .param(PastaParam.floatField("thickness", siblings -> 2.5f))
                .param(PastaParam.floatSlider("height", (random, siblings) -> siblings.f("diameter") * random.triangle(1.25f, 0.2f), siblings -> new PastaParam.Bounds(siblings.f("diameter") * 1.05f, siblings.f("diameter") * 1.45f)))
                .param(PastaParam.floatSlider("taper_factor", (random, siblings) -> Mth.lerp(random.nextFloat(), 2, 3), siblings -> new PastaParam.Bounds(2, 3)))
                .param(PastaParam.rotation(helper))
                .param(PastaParam.pasta(helper))
                .param(PastaParam.sauce(helper))
                .place((level, center, params) -> shellPlacer.placeShell(level, center, params.rotation(), params.pasta(), params.sauce(), params.f("height"), params.f("diameter"), params.f("taper_factor"), params.f("thickness")));
    }

    public void placeShell(LevelSimulatedRW level, Vec3 center, Quaterniond rotation, BlockState pasta, BlockState sauce, float height, float diameter, float taperFactor, float thickness) {
        float halfHeight = height * 0.65f; // Slightly over half, or else orphaned voxels (blocks) will place
        float radius = diameter * 0.5f;
        InnerSpaceIterable spaceIterator = InnerSpaceIterable.centered(center, rotation, diameter, height, diameter);
        for (InnerSpacePos innerSpacePos : spaceIterator) {
            double sampledRadian = Mth.atan2(innerSpacePos.innerZ(), innerSpacePos.innerX()) + Mth.PI;
            float sampledRadius = Mth.length(innerSpacePos.innerX(), innerSpacePos.innerZ());
            double taper = Math.pow(1 - Mth.square(innerSpacePos.innerY() / halfHeight), taperFactor);

            double radiusThreshold = Math.sqrt(sampledRadian / Mth.TWO_PI) * radius * taper; // Offset of shell manifold from inner-space's Y axis
            boolean solid = sampledRadius < radiusThreshold && sampledRadius > radiusThreshold - thickness;
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
