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

public class SpiralPlacer {

    private final Util helper;

    public SpiralPlacer(Util helper) {
        this.helper = helper;
    }

    public void placeRandomSpiral(ServerLevel level, BlockPos centerPos) {
        RandomSource random = level.getRandom().forkPositional().at(centerPos);

        float diameter = Mth.lerp(random.nextFloat(), 12, 16);
        float height = Mth.lerp(random.nextFloat(), 50, 80);
        float thickness = Mth.lerp(Mth.square(random.nextFloat()), 2f, diameter * 0.25f);
        float twist = 1f / Mth.lerp(random.nextFloat(), 3f, 8f);

        this.placeSpiral(level, diameter, new Vec3(centerPos), this.helper.nextRotation(random), height, thickness, twist, this.helper.getRandomPastaBlock(random));
    }

    private void placeSpiral(ServerLevel level, float diameter, Vec3 center, Quaterniond rotation, float height, float thickness, float twist, BlockState pasta) {
        float radius = diameter * 0.5f;
        InnerSpaceIterable spaceIterator = InnerSpaceIterable.centered(center, rotation, diameter, height, diameter);
        for (InnerSpacePos innerSpacePos : spaceIterator) {
            float sampledRadius = Mth.length(innerSpacePos.innerX(), innerSpacePos.innerZ());

            boolean solid = sampledRadius < radius && thickness > Mth.abs(Mth.cos(innerSpacePos.innerY() * twist) * innerSpacePos.innerZ() - Mth.sin(innerSpacePos.innerY() * twist) * innerSpacePos.innerX());
            if (solid) level.setBlock(innerSpacePos.toBlockPos(), pasta, Block.UPDATE_ALL);
        }
    }

}
