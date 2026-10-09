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
        float thickness = 2.5f;
        float height = diameter * random.triangle(1.25f, 0.2f);
        float halfHeight = height * 0.65f; // Slightly over half, or else orphaned voxels (blocks) will place
        float taperFactor = Mth.lerp(random.nextFloat(), 2, 3);

        InnerSpaceIterable spaceIterator = InnerSpaceIterable.centered(new Vec3(centerPos), this.helper.nextRotation(random), diameter, height, diameter);
        for (InnerSpacePos innerSpacePos : spaceIterator) {
            double sampledRadian = Mth.atan2(innerSpacePos.innerZ(), innerSpacePos.innerX()) + Mth.PI;
            float sampledRadius = Mth.length(innerSpacePos.innerX(), innerSpacePos.innerZ());
            double taper = Math.pow(1 - Mth.square(innerSpacePos.innerY() / halfHeight), taperFactor);

            double radiusThreshold = Math.sqrt(sampledRadian / Mth.TWO_PI) * radius * taper; // Offset of shell manifold from inner-space's Y axis
            boolean solid = sampledRadius < radiusThreshold && sampledRadius > radiusThreshold - thickness;
            if (solid) level.setBlock(innerSpacePos.toBlockPos(), pasta, Block.UPDATE_ALL);
        }
    }

    public void placeRadiatori(ServerLevel level, BlockPos centerPos) {
        RandomSource random = level.getRandom().forkPositional().at(centerPos);

        BlockState pasta = this.helper.getRandomPastaBlock(random);

        // Each radiatori piece curls differently, and not as aggressively to become a pasta conch (shell).
        float diameterHead = Mth.lerp(random.nextFloat(), 6, 12), diameterTail = Mth.lerp(random.nextFloat(), 6, 12);
        float radiusHead = diameterHead * 0.5f, radiusTail = diameterTail * 0.5f;
        float coreThickness = 2.5f;

        // Radiatori gets its name from looking like radiators!
        int fins = random.nextIntBetweenInclusive(3, 6); // Fins for capturing sauce
        float finWidth = 3f; // How thick each fin should be.
        float finSpacing = 3.5f; // Size of gap between fins.
        float finUnit = finWidth + finSpacing;
        float length = fins * finUnit + finSpacing; // Total length of radiatori from fin count, plus extra spacer.
        float lengthHalf = length * 0.5f;
        float finHeight = Mth.lerp(random.nextFloat(), 4, 6); // How tall the fins are from the base radius, additively.

        // Pasta Extruders don't always push mass evenly, so straight fins can distort into frills.
        float finFrillingFreq = Mth.lerp(random.nextFloat(), 2, 10);
        // Flowrate in extrusion may change, so one end may have greater frilling than the other.
        float finFrillingAmpHead = Mth.lerp(random.nextFloat(), 0, 1.5f), finFrillingAmpTail = Mth.lerp(random.nextFloat(), 0, 1.5f);

        // Radiatori shouldn't close as a circle, but more often than not comes close. Pow2 biases for nearing a complete circle.
        float cutoffRadian = Mth.lerp(Mth.square(random.nextFloat()), 0.9f, 0.6f) * Mth.TWO_PI;

        float diameterMax = Math.max(diameterHead, diameterTail) + finHeight * 2f;

        InnerSpaceIterable spaceIterator = InnerSpaceIterable.centered(new Vec3(centerPos), this.helper.nextRotation(random), diameterMax, length, diameterMax);
        for (InnerSpacePos innerSpacePos : spaceIterator) {
            // Clockwise angle around Y-Axis of the inner space, ranging [0, 2PI)
            double sampledRadian = Mth.atan2(innerSpacePos.innerZ(), -innerSpacePos.innerX()) + Mth.PI;
            if (sampledRadian > cutoffRadian) continue;

            float sampledRadius = Mth.length(innerSpacePos.innerX(), innerSpacePos.innerZ());

            // Percentile of arc progression, clockwise from X-axis positive towards Z-axis negative.
            double arcLerpFactor = sampledRadian / cutoffRadian;

            // Radius at given point of arc around Y-axis (for the pasta's slight curling)
            double radiusThreshold = Mth.lerp(arcLerpFactor, radiusHead, radiusTail);

            // Y-offset to produce the frilling in the fin checks.
            double frillingYOffset = Mth.cos(sampledRadian * finFrillingFreq) // The wave for the frills.
                    * Mth.lerp(arcLerpFactor, finFrillingAmpHead, finFrillingAmpTail) // Amplitude of frilling, in respect to arc progression.
                    * Mth.inverseLerp(sampledRadius, radiusThreshold, radiusThreshold + finHeight); // Flatten frills towards the core, keeping full frilling at the extremity.

            double yAdjusted = frillingYOffset + innerSpacePos.innerY() + lengthHalf; // Adjusted y pos for checking if within frill in pasta shape.
            int fin = Mth.floor(yAdjusted / finUnit); // Which fin, from the bottom, that the Y-pos samples. In respect to the frilling.
            boolean isYWithinFin = (yAdjusted % finUnit > finSpacing) // True if the adjusted Y-pos outside the fin gap, moduled per fin unit.
                    && fin >= 0 && fin < fins; // Avoid extra fins/stray voxels via rounding error.

            boolean solid = sampledRadius > radiusThreshold - coreThickness && sampledRadius < radiusThreshold + (isYWithinFin ? finHeight : 0f);
            if (solid) level.setBlock(innerSpacePos.toBlockPos(), pasta, Block.UPDATE_ALL);
        }
    }

}
