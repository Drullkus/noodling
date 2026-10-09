package us.drullk.noodling.pasta;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import us.drullk.noodling.shape.InnerSpaceIterable;
import us.drullk.noodling.shape.InnerSpacePos;
import us.drullk.noodling.util.Util;

public class PastaPlacers {

    public void placePenne(ServerLevel level, BlockPos centerPos) {
        RandomSource random = level.getRandom().forkPositional().at(centerPos);

        BlockState pasta = Util.getRandomBlock(random).value().defaultBlockState();

        float diameter = Mth.lerp(random.nextFloat(), 12, 16);
        float radius = diameter * 0.5f;
        float height = Mth.lerp(random.nextFloat(), 50, 80);

        InnerSpaceIterable spaceIterator = InnerSpaceIterable.centered(new Vec3(centerPos), Util.nextRotation(random), diameter, height, diameter);
        for (InnerSpacePos innerSpacePos : spaceIterator) {
            float shapeRadius = Mth.length(innerSpacePos.innerX(), innerSpacePos.innerZ());

            boolean solid = shapeRadius > radius * 0.667f && shapeRadius < radius && 0 > Math.abs(innerSpacePos.innerX() - innerSpacePos.innerY()) * 2 + diameter - height;
            if (solid) level.setBlock(innerSpacePos.toBlockPos(), pasta, Block.UPDATE_ALL);
        }
    }

    public void placeSpiral(ServerLevel level, BlockPos centerPos) {
        RandomSource random = level.getRandom().forkPositional().at(centerPos);

        BlockState pasta = Util.getRandomBlock(random).value().defaultBlockState();

        float diameter = Mth.lerp(random.nextFloat(), 12, 16);
        float radius = diameter * 0.5f;
        float height = Mth.lerp(random.nextFloat(), 50, 80);
        float thickness = Mth.lerp(Mth.square(random.nextFloat()), 2f, diameter * 0.25f);
        float twist = 1f / Mth.lerp(random.nextFloat(), 3f, 8f);

        InnerSpaceIterable spaceIterator = InnerSpaceIterable.centered(new Vec3(centerPos), Util.nextRotation(random), diameter, height, diameter);
        for (InnerSpacePos innerSpacePos : spaceIterator) {
            float shapeRadius = Mth.length(innerSpacePos.innerX(), innerSpacePos.innerZ());

            boolean solid = shapeRadius < radius && thickness > Mth.abs(Mth.cos(innerSpacePos.innerY() * twist) * innerSpacePos.innerZ() - Mth.sin(innerSpacePos.innerY() * twist) * innerSpacePos.innerX());
            if (solid) level.setBlock(innerSpacePos.toBlockPos(), pasta, Block.UPDATE_ALL);
        }
    }

    public void placeShell(ServerLevel level, BlockPos centerPos) {
        RandomSource random = level.getRandom().forkPositional().at(centerPos);

        BlockState pasta = Util.getRandomBlock(random).value().defaultBlockState();

        float diameter = Mth.lerp(random.nextFloat(), 16, 20);
        float radius = diameter * 0.5f;
        float height = diameter * random.triangle(1.25f, 0.2f);
        float halfHeight = height * 0.65f; // Slightly over half, or else orphaned voxels (blocks) will place
        float taperFactor = Mth.lerp(random.nextFloat(), 2, 3);

        InnerSpaceIterable spaceIterator = InnerSpaceIterable.centered(new Vec3(centerPos), Util.nextRotation(random), diameter, height, diameter);
        for (InnerSpacePos innerSpacePos : spaceIterator) {
            double angle = Mth.atan2(innerSpacePos.innerZ(), innerSpacePos.innerX()) + Mth.PI;
            float shapeRadius = Mth.length(innerSpacePos.innerX(), innerSpacePos.innerZ());
            double taper = Math.pow(1 - Mth.square(innerSpacePos.innerY() / halfHeight), taperFactor);

            double angleThreshold = Math.sqrt(angle) / Mth.PI * radius * taper;
            boolean solid = shapeRadius < angleThreshold && shapeRadius > angleThreshold - 2.5f;
            if (solid) level.setBlock(innerSpacePos.toBlockPos(), pasta, Block.UPDATE_ALL);
        }
    }

}
