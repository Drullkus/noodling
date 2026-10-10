package us.drullk.noodling.noodler;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Finds where the Noodler places pasta: the first block, fluid surface or entity along the holder's view line, or
 * the end of the line when nothing is hit.
 *
 * <p>Written by Claude Code; conversation transcript: {@code llm/transcripts/transcript_2026-10-10_25b84f50-3f88-41a1-a942-98824ffe67dc.md}.
 */
public final class NoodlerRaycast {

    /** How far the Noodler reaches, in blocks. */
    public static final double RANGE = 128.0;

    private NoodlerRaycast() {
    }

    /**
     * Casts from the eyes of {@code source} along its view vector. The result is never a miss in the sense that
     * {@link HitResult#getLocation()} is always usable: for a miss it is the point {@link #RANGE} blocks away.
     */
    public static HitResult raycast(Entity source, float partialTick) {
        Vec3 from = source.getEyePosition(partialTick);
        Vec3 direction = source.getViewVector(partialTick);
        Vec3 to = from.add(direction.scale(RANGE));

        HitResult blockHit = source.level().clip(new ClipContext(from, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.ANY, source));
        double maxDistanceSq = blockHit.getType() == HitResult.Type.MISS ? RANGE * RANGE : blockHit.getLocation().distanceToSqr(from);

        AABB searchArea = source.getBoundingBox().expandTowards(direction.scale(RANGE)).inflate(1.0);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(source, from, to, searchArea,
                entity -> entity != source && entity.isPickable() && !entity.isSpectator(), maxDistanceSq);
        return entityHit != null ? entityHit : blockHit;
    }
}
