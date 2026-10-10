package us.drullk.noodling.pasta;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import us.drullk.noodling.shape.InnerSpaceIterable;
import us.drullk.noodling.shape.InnerSpacePos;
import us.drullk.noodling.util.Util;

public class PipettePlacer {

    private final Util helper;

    public PipettePlacer(Util helper) {
        this.helper = helper;
    }

    public void placeRandomPipette(ServerLevel level, BlockPos centerPos) {
        RandomSource random = level.getRandom().forkPositional().at(centerPos);

        float diameter = 14;
        float radius = diameter * 0.5f;
        float pinchRadiusHead = Mth.lerp(random.nextFloat(), radius * 0.5f, radius), pinchRadiusTail = Mth.lerp(random.nextFloat(), radius * 0.5f, radius);
        float height = 25;
        float thickness = 3.125f;
        float curve = height * 0.75f;

        this.placePipette(level, new Vec3(centerPos), this.helper.nextRotation(random), this.helper.getRandomPastaBlock(random), diameter, height, curve, radius, pinchRadiusHead, pinchRadiusTail, thickness);
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
