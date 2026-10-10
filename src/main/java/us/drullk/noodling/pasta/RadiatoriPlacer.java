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

public class RadiatoriPlacer {

    private final Util helper;

    public RadiatoriPlacer(Util helper) {
        this.helper = helper;
    }

    public void placeRandomRadiatori(ServerLevel level, Vec3 center, RandomSource random) {
        // Each radiatori piece curls differently, and not as aggressively to become a pasta conch (shell).
        float diameterHead = Mth.lerp(random.nextFloat(), 6, 12), diameterTail = Mth.lerp(random.nextFloat(), 6, 12);
        float coreThickness = 2.5f;

        // Radiatori gets its name from looking like radiators!
        int fins = random.nextIntBetweenInclusive(3, 6); // Fins for capturing sauce
        float finWidth = 3f; // How thick each fin should be.
        float finSpacing = 3.5f; // Size of gap between fins.
        float finHeight = Mth.lerp(random.nextFloat(), 4, 6); // How tall the fins are from the base radius, additively.

        // Pasta Extruders don't always push mass evenly, so straight fins can distort into frills.
        // Flowrate in extrusion may change, so one end may have greater frilling than the other. Varies both by frequency and amplitude!
        float finFrillingFreqHead = Mth.lerp(Mth.square(random.nextFloat()), 6, 2), finFrillingFreqTail = Mth.lerp(Mth.square(random.nextFloat()), 6, 2);
        float finFrillingAmpHead = Mth.lerp(Mth.square(random.nextFloat()), 1.5f, 0), finFrillingAmpTail = Mth.lerp(Mth.square(random.nextFloat()), 1.5f, 0);

        // Radiatori shouldn't close as a circle, but more often than not comes close. Pow2 biases for nearing a complete circle.
        float cutoffRadian = Mth.lerp(Mth.square(random.nextFloat()), 0.9f, 2f/3f) * Mth.TWO_PI;

        this.placeRadiatori(level, center, this.helper.nextRotation(random), this.helper.getRandomPastaBlock(random), diameterHead, diameterTail, finHeight, finWidth, finSpacing, fins, cutoffRadian, finFrillingFreqHead, finFrillingFreqTail, finFrillingAmpHead, finFrillingAmpTail, coreThickness);
    }

    public void placeRadiatori(ServerLevel level, Vec3 center, Quaterniond rotation, BlockState pasta, float diameterHead, float diameterTail, float finHeight, float finWidth, float finSpacing, int fins, float cutoffRadian, float finFrillingFreqHead, float finFrillingFreqTail, float finFrillingAmpHead, float finFrillingAmpTail, float coreThickness) {
        float radiusHead = diameterHead * 0.5f, radiusTail = diameterTail * 0.5f;
        float diameterMax = Math.max(diameterHead, diameterTail) + finHeight * 2f;
        float finUnit = finWidth + finSpacing;
        float length = fins * finUnit + finSpacing; // Total length of radiatori from fin count, plus extra spacer.
        float lengthHalf = length * 0.5f;
        InnerSpaceIterable spaceIterator = InnerSpaceIterable.centered(center, rotation, diameterMax, length, diameterMax);
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
            double frillingYOffset = Mth.cos(sampledRadian * Mth.lerp(arcLerpFactor, finFrillingFreqHead, finFrillingFreqTail)) // The wave for the frills.
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
