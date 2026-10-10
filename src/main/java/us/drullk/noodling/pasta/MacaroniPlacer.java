package us.drullk.noodling.pasta;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Ease;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import us.drullk.noodling.shape.InnerSpaceIterable;
import us.drullk.noodling.shape.InnerSpacePos;
import us.drullk.noodling.util.Util;

public class MacaroniPlacer {

    private final Util helper;

    public MacaroniPlacer(Util helper) {
        this.helper = helper;
    }

    public void placeRandomMacaroni(ServerLevel level, Vec3 center, RandomSource random) {
        float diameter = Mth.lerp(random.nextFloat(), 8, 10);
        float height = random.triangle(35, 5);
        float thickness = 2.5f;
        float curve = Mth.lerp(Ease.inOutQuad(random.nextFloat()), height * 0.75f, height * 2f);

        this.placeMacaroni(level, center, this.helper.nextRotation(random), this.helper.getRandomPastaBlock(random), diameter, height, curve, thickness);
    }

    public void placeMacaroni(ServerLevel level, Vec3 center, Quaterniond rotation, BlockState pasta, float diameter, float height, float curve, float thickness) {
        float radius = diameter * 0.5f;
        InnerSpaceIterable spaceIterator = InnerSpaceIterable.centered(center, rotation, diameter, height, diameter);
        for (InnerSpacePos innerSpacePos : spaceIterator.bentByRadius(curve)) {
            float sampledRadius = Mth.length(innerSpacePos.innerX(), innerSpacePos.innerZ());

            boolean solid = sampledRadius > radius - thickness && sampledRadius < radius;
            if (solid) level.setBlock(innerSpacePos.toBlockPos(), pasta, Block.UPDATE_ALL);
        }
    }

}
