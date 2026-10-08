package us.drullk.impastable.shape;

import net.minecraft.core.BlockPos;

/**
 * A world block together with where that block's centre lands inside an {@link InnerSpaceIterable}.
 *
 * <p>{@code worldX/Y/Z} are ordinary block coordinates. {@code innerX/Y/Z} are the same point expressed in the
 * inner (shape) coordinate system of the space that produced this record, so a shape function can decide whether
 * this block is pasta by looking only at the inner coordinates (distance from the inner axis, angle around it,
 * position along it, ...).
 *
 * <p>Written by Claude Code; conversation transcript: {@code llm/transcripts/transcript_2026-10-06_17647dd8-8de1-4b0b-b68d-89a2d0ee63a8.md}.
 */
public record InnerSpacePos(int worldX, int worldY, int worldZ, float innerX, float innerY, float innerZ) {

    public BlockPos toBlockPos() {
        return new BlockPos(worldX, worldY, worldZ);
    }
}
