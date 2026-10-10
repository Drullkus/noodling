package us.drullk.noodling.pasta;

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

public class ShellPlacer {

    private final Util helper;

    public ShellPlacer(Util helper) {
        this.helper = helper;
    }

    public void placeRandomShell(ServerLevel level, Vec3 center, RandomSource random) {
        float diameter = Mth.lerp(random.nextFloat(), 12, 16);
        float thickness = 2.5f;
        float height = diameter * random.triangle(1.25f, 0.2f);
        float taperFactor = Mth.lerp(random.nextFloat(), 2, 3);

        this.placeShell(level, center, this.helper.nextRotation(random), this.helper.getRandomPastaBlock(random), height, diameter, taperFactor, thickness);
    }

    public void placeShell(ServerLevel level, Vec3 center, Quaterniond rotation, BlockState pasta, float height, float diameter, float taperFactor, float thickness) {
        float halfHeight = height * 0.65f; // Slightly over half, or else orphaned voxels (blocks) will place
        float radius = diameter * 0.5f;
        InnerSpaceIterable spaceIterator = InnerSpaceIterable.centered(center, rotation, diameter, height, diameter);
        for (InnerSpacePos innerSpacePos : spaceIterator) {
            double sampledRadian = Mth.atan2(innerSpacePos.innerZ(), innerSpacePos.innerX()) + Mth.PI;
            float sampledRadius = Mth.length(innerSpacePos.innerX(), innerSpacePos.innerZ());
            double taper = Math.pow(1 - Mth.square(innerSpacePos.innerY() / halfHeight), taperFactor);

            double radiusThreshold = Math.sqrt(sampledRadian / Mth.TWO_PI) * radius * taper; // Offset of shell manifold from inner-space's Y axis
            boolean solid = sampledRadius < radiusThreshold && sampledRadius > radiusThreshold - thickness;
            if (solid) level.setBlock(innerSpacePos.toBlockPos(), pasta, Block.UPDATE_ALL);
        }
    }

}
