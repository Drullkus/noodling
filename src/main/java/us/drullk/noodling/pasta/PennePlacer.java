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

public class PennePlacer {

    private final Util helper;

    public PennePlacer(Util helper) {
        this.helper = helper;
    }

    public void placeRandomPenne(ServerLevel level, BlockPos centerPos) {
        RandomSource random = level.getRandom().forkPositional().at(centerPos);

        float diameter = Mth.lerp(random.nextFloat(), 12, 16);
        float height = Mth.lerp(random.nextFloat(), 50, 80);

        this.placePenne(level, new Vec3(centerPos), this.helper.nextRotation(random), this.helper.getRandomPastaBlock(random), diameter, height);
    }

    public void placePenne(ServerLevel level, Vec3 center, Quaterniond rotation, BlockState pasta, float diameter, float height) {
        float radius = diameter * 0.5f;
        InnerSpaceIterable spaceIterator = InnerSpaceIterable.centered(center, rotation, diameter, height, diameter);
        for (InnerSpacePos innerSpacePos : spaceIterator) {
            float sampledRadius = Mth.length(innerSpacePos.innerX(), innerSpacePos.innerZ());

            boolean solid = sampledRadius > radius * 0.667f && sampledRadius < radius // Filter positions for inside and outside the hollow cylinder
                    && 0 > Math.abs(innerSpacePos.innerX() - innerSpacePos.innerY()) * 2 + diameter - height; // Angled cutoffs for the ends
            if (solid) level.setBlock(innerSpacePos.toBlockPos(), pasta, Block.UPDATE_ALL);
        }
    }

}
