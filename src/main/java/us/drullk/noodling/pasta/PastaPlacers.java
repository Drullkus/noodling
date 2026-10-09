package us.drullk.noodling.pasta;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import us.drullk.noodling.shape.InnerSpaceIterable;
import us.drullk.noodling.shape.InnerSpacePos;
import us.drullk.noodling.util.Util;

public class PastaPlacers {

    private Util helper = new Util();

    public void placePenne(ServerLevel level, BlockPos centerPos) {
        RandomSource random = level.getRandom().forkPositional().at(centerPos);

        BlockState pasta = this.helper.getRandomPastaBlock(random);

        float diameter = Mth.lerp(random.nextFloat(), 12, 16);
        float radius = diameter * 0.5f;
        float height = Mth.lerp(random.nextFloat(), 50, 80);

        InnerSpaceIterable spaceIterator = InnerSpaceIterable.centered(new Vec3(centerPos), this.helper.nextRotation(random), diameter, height, diameter);
        for (InnerSpacePos innerSpacePos : spaceIterator) {
            float sampledRadius = Mth.length(innerSpacePos.innerX(), innerSpacePos.innerZ());

            boolean solid = sampledRadius > radius * 0.667f && sampledRadius < radius && 0 > Math.abs(innerSpacePos.innerX() - innerSpacePos.innerY()) * 2 + diameter - height;
            if (solid) level.setBlock(innerSpacePos.toBlockPos(), pasta, Block.UPDATE_ALL);
        }
    }

    public void placeSpiral(ServerLevel level, BlockPos centerPos) {
        RandomSource random = level.getRandom().forkPositional().at(centerPos);

        BlockState pasta = this.helper.getRandomPastaBlock(random);

        float diameter = Mth.lerp(random.nextFloat(), 12, 16);
        float radius = diameter * 0.5f;
        float height = Mth.lerp(random.nextFloat(), 50, 80);
        float thickness = Mth.lerp(Mth.square(random.nextFloat()), 2f, diameter * 0.25f);
        float twist = 1f / Mth.lerp(random.nextFloat(), 3f, 8f);

        InnerSpaceIterable spaceIterator = InnerSpaceIterable.centered(new Vec3(centerPos), this.helper.nextRotation(random), diameter, height, diameter);
        for (InnerSpacePos innerSpacePos : spaceIterator) {
            float sampledRadius = Mth.length(innerSpacePos.innerX(), innerSpacePos.innerZ());

            boolean solid = sampledRadius < radius && thickness > Mth.abs(Mth.cos(innerSpacePos.innerY() * twist) * innerSpacePos.innerZ() - Mth.sin(innerSpacePos.innerY() * twist) * innerSpacePos.innerX());
            if (solid) level.setBlock(innerSpacePos.toBlockPos(), pasta, Block.UPDATE_ALL);
        }
    }

    public void placeShell(ServerLevel level, BlockPos centerPos) {
        RandomSource random = level.getRandom().forkPositional().at(centerPos);

        BlockState pasta = this.helper.getRandomPastaBlock(random);

        float diameter = Mth.lerp(random.nextFloat(), 12, 16);
        float radius = diameter * 0.5f;
        float height = diameter * random.triangle(1.25f, 0.2f);
        float halfHeight = height * 0.65f; // Slightly over half, or else orphaned voxels (blocks) will place
        float taperFactor = Mth.lerp(random.nextFloat(), 2, 3);

        InnerSpaceIterable spaceIterator = InnerSpaceIterable.centered(new Vec3(centerPos), this.helper.nextRotation(random), diameter, height, diameter);
        for (InnerSpacePos innerSpacePos : spaceIterator) {
            double sampledRadian = Mth.atan2(innerSpacePos.innerZ(), innerSpacePos.innerX()) + Mth.PI;
            float sampledRadius = Mth.length(innerSpacePos.innerX(), innerSpacePos.innerZ());
            double taper = Math.pow(1 - Mth.square(innerSpacePos.innerY() / halfHeight), taperFactor);

            double radiusThreshold = Math.sqrt(sampledRadian / Mth.TWO_PI) * radius * taper; // Offset of shell manifold from inner-space's Y axis
            boolean solid = sampledRadius < radiusThreshold && sampledRadius > radiusThreshold - 2.5f;
            if (solid) level.setBlock(innerSpacePos.toBlockPos(), pasta, Block.UPDATE_ALL);
        }
    }

    public void placeRadiatori(ServerLevel level, BlockPos centerPos) {
        RandomSource random = level.getRandom().forkPositional().at(centerPos);

        BlockState pasta = this.helper.getRandomPastaBlock(random);

        int fins = random.nextIntBetweenInclusive(3, 6);
        float finWidth = 3f;
        float finSpacing = 3.5f;
        float finUnit = finWidth + finSpacing;
        float length = fins * finUnit + finSpacing;
        float lengthHalf = length * 0.5f;
        float finFrillingFreq = Mth.lerp(random.nextFloat(), 2, 10);
        float finFrillingAmpHead = Mth.lerp(random.nextFloat(), 0, 1.5f);
        float finFrillingAmpTail = Mth.lerp(random.nextFloat(), 0, 1.5f);

        float finHeight = Mth.lerp(random.nextFloat(), 4, 6);

        float diameterHead = Mth.lerp(random.nextFloat(), 6, 12);
        float diameterTail = Mth.lerp(random.nextFloat(), 6, 12);
        float radiusHead = diameterHead * 0.5f;
        float radiusTail = diameterTail * 0.5f;

        float diameterMax = Math.max(diameterHead, diameterTail) + finHeight * 2f;

        float cutoffRadian = Mth.lerp(Mth.square(random.nextFloat()), 0.9f, 0.6f) * Mth.TWO_PI;

        InnerSpaceIterable spaceIterator = InnerSpaceIterable.centered(new Vec3(centerPos), this.helper.nextRotation(random), diameterMax, length, diameterMax);
        for (InnerSpacePos innerSpacePos : spaceIterator) {
            double sampledRadian = Mth.atan2(innerSpacePos.innerZ(), innerSpacePos.innerX()) + Mth.PI;

            if (sampledRadian > cutoffRadian) continue;

            float sampledRadius = Mth.length(innerSpacePos.innerX(), innerSpacePos.innerZ());

            double arcLerpFactor = sampledRadian / cutoffRadian;
            double radiusThreshold = Mth.lerp(arcLerpFactor, radiusHead, radiusTail);
            double frillingYOffset = Mth.cos(sampledRadian * finFrillingFreq) * Mth.lerp(arcLerpFactor, finFrillingAmpHead, finFrillingAmpTail) * Mth.inverseLerp(sampledRadius, radiusThreshold, radiusThreshold + finHeight);

            boolean isYWithinFin = (frillingYOffset + innerSpacePos.innerY() + lengthHalf) % finUnit > finSpacing;
            int fin = Mth.floor((frillingYOffset + innerSpacePos.innerY() + lengthHalf) / finUnit);
            boolean solid = sampledRadius > radiusThreshold - 2.5f && sampledRadius < radiusThreshold + (isYWithinFin && fin >= 0 && fin < fins ? finHeight : 0f);
            if (solid) level.setBlock(innerSpacePos.toBlockPos(), pasta, Block.UPDATE_ALL);
            // else level.setBlock(innerSpacePos.toBlockPos(), Blocks.STAINED_GLASS.red().defaultBlockState(), Block.UPDATE_ALL);
        }
    }

}
