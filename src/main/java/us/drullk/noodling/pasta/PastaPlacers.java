package us.drullk.noodling.pasta;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import us.drullk.noodling.Noodling;
import us.drullk.noodling.shape.InnerSpaceIterable;
import us.drullk.noodling.shape.InnerSpacePos;

public class PastaPlacers {

    public Holder<Block> getRandomBlock(RandomSource random) {
        return Util.getRandom(Noodling.PASTA_BLOCKS, random);
    }

    public void placePenne(ServerLevel level, BlockPos centerPos) {
        RandomSource random = level.getRandom().forkPositional().at(centerPos);

        BlockState pasta = this.getRandomBlock(random).value().defaultBlockState();

        float diameter = Mth.lerp(random.nextFloat(), 12, 16);
        float radius = diameter * 0.5f;
        float height = Mth.lerp(random.nextFloat(), 50, 80);

        Quaterniond rotation = new Quaterniond().rotationXYZ(random.triangle(0, 180f), random.triangle(0, 180f), random.triangle(0, 180f));

        InnerSpaceIterable spaceIterator = InnerSpaceIterable.centered(new Vec3(centerPos), rotation, diameter, height, diameter);
        for (InnerSpacePos innerSpacePos : spaceIterator) {
            float shapeRadius = Mth.sqrt(innerSpacePos.innerX() * innerSpacePos.innerX() + innerSpacePos.innerZ() * innerSpacePos.innerZ());

            boolean solid = shapeRadius > radius * 0.667f && shapeRadius < radius && 0 > Math.abs(innerSpacePos.innerX() - innerSpacePos.innerY()) * 2 + diameter - height;
            if (solid) level.setBlock(innerSpacePos.toBlockPos(), pasta, Block.UPDATE_ALL);
        }
    }

    public void placeSpiral(ServerLevel level, BlockPos centerPos) {
        RandomSource random = level.getRandom().forkPositional().at(centerPos);

        BlockState pasta = this.getRandomBlock(random).value().defaultBlockState();

        float diameter = Mth.lerp(random.nextFloat(), 12, 16);
        float radius = diameter * 0.5f;
        float height = Mth.lerp(random.nextFloat(), 50, 80);
        float thickness = Mth.lerp(Mth.square(random.nextFloat()), 2f, diameter * 0.25f);
        float twist = 1f / Mth.lerp(random.nextFloat(), 3f, 8f);

        Quaterniond rotation = new Quaterniond().rotationXYZ(random.triangle(0, 180f), random.triangle(0, 180f), random.triangle(0, 180f));

        InnerSpaceIterable spaceIterator = InnerSpaceIterable.centered(new Vec3(centerPos), rotation, diameter, height, diameter);
        for (InnerSpacePos innerSpacePos : spaceIterator) {
            float shapeRadius = Mth.sqrt(innerSpacePos.innerX() * innerSpacePos.innerX() + innerSpacePos.innerZ() * innerSpacePos.innerZ());

            boolean solid = shapeRadius < radius && thickness > Mth.abs(Mth.cos(innerSpacePos.innerY() * twist) * innerSpacePos.innerZ() - Mth.sin(innerSpacePos.innerY() * twist) * innerSpacePos.innerX());
            if (solid) level.setBlock(innerSpacePos.toBlockPos(), pasta, Block.UPDATE_ALL);
        }
    }

}
