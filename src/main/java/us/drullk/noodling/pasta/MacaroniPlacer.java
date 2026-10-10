package us.drullk.noodling.pasta;

import net.minecraft.world.level.LevelWriter;
import net.minecraft.util.Ease;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import us.drullk.noodling.shape.InnerSpaceIterable;
import us.drullk.noodling.shape.InnerSpacePos;
import org.jspecify.annotations.Nullable;
import us.drullk.noodling.noodler.PastaParam;
import us.drullk.noodling.noodler.PastaShape;
import us.drullk.noodling.util.Util;

public class MacaroniPlacer {

    private final Util helper;
    private @Nullable PastaShape shape;

    public MacaroniPlacer(Util helper) {
        this.helper = helper;
    }

    /**
     * Places a macaroni with every parameter rolled from {@code random}, through {@link #shape()}.
     *
     * <p>The random calls happen in the same order as before, so this method still gives the same shape for the same
     * random source; the Noodler resolves the same parameters from its own seeds instead.
     *
     * <p>Written by Claude Code; conversation transcript: {@code llm/transcripts/transcript_2026-10-10_25b84f50-3f88-41a1-a942-98824ffe67dc.md}.
     */
    public void placeRandomMacaroni(LevelWriter level, Vec3 center, RandomSource random) {
        this.shape().placeRandom(level, center, random);
    }

    /**
     * Describes macaroni for the Noodler. The curve slider's bounds follow the height.
     *
     * <p>Written by Claude Code; conversation transcript: {@code llm/transcripts/transcript_2026-10-10_25b84f50-3f88-41a1-a942-98824ffe67dc.md}.
     */
    public PastaShape shape() {
        if (this.shape == null) {
            this.shape = PastaShape.builder("macaroni")
                    .param(PastaParam.floatSlider("diameter", (random, siblings) -> Mth.lerp(random.nextFloat(), 8, 10), siblings -> new PastaParam.Bounds(8, 10)))
                    .param(PastaParam.floatSlider("height", (random, siblings) -> random.triangle(35, 5), siblings -> new PastaParam.Bounds(30, 40)))
                    .param(PastaParam.floatField("thickness", siblings -> 2.5f))
                    .param(PastaParam.floatSlider("curve", (random, siblings) -> Mth.lerp(Ease.inOutQuad(random.nextFloat()), siblings.f("height") * 0.75f, siblings.f("height") * 2f), siblings -> new PastaParam.Bounds(siblings.f("height") * 0.75f, siblings.f("height") * 2f)))
                    .param(PastaParam.rotation(this.helper))
                    .param(PastaParam.pasta(this.helper))
                    .place((level, center, params) -> this.placeMacaroni(level, center, params.rotation(), params.pasta(), params.f("diameter"), params.f("height"), params.f("curve"), params.f("thickness")));
        }
        return this.shape;
    }

    public void placeMacaroni(LevelWriter level, Vec3 center, Quaterniond rotation, BlockState pasta, float diameter, float height, float curve, float thickness) {
        float radius = diameter * 0.5f;
        InnerSpaceIterable spaceIterator = InnerSpaceIterable.centered(center, rotation, diameter, height, diameter);
        for (InnerSpacePos innerSpacePos : spaceIterator.bentByRadius(curve)) {
            float sampledRadius = Mth.length(innerSpacePos.innerX(), innerSpacePos.innerZ());

            boolean solid = sampledRadius > radius - thickness && sampledRadius < radius;
            if (solid) level.setBlock(innerSpacePos.toBlockPos(), pasta, Block.UPDATE_ALL);
        }
    }

}
