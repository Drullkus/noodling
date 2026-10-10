package us.drullk.noodling.noodler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelSimulatedRW;
import net.minecraft.world.level.LevelWriter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;

/**
 * A {@link LevelSimulatedRW} that keeps every block a placer writes instead of changing a world, and turns the result
 * into a structure template tag in the same layout {@code StructureTemplate#save} produces (size, palette, blocks,
 * entities, DataVersion). Positions are shifted so the lowest corner is the origin; cells nothing wrote are left out
 * of the block list, which a structure template treats as structure void.
 *
 * <p>Written by Claude Code; conversation transcript: {@code llm/transcripts/transcript_2026-10-10_25b84f50-3f88-41a1-a942-98824ffe67dc.md}.
 */
public final class RecordingLevelWriter implements LevelSimulatedRW {

    private final Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
    private @Nullable BlockPos min;
    private @Nullable BlockPos max;

    @Override
    public boolean setBlock(BlockPos pos, BlockState blockState, int updateFlags, int updateLimit) {
        BlockPos key = pos.immutable();
        this.blocks.put(key, blockState);
        this.min = this.min == null ? key : new BlockPos(Math.min(this.min.getX(), key.getX()), Math.min(this.min.getY(), key.getY()), Math.min(this.min.getZ(), key.getZ()));
        this.max = this.max == null ? key : new BlockPos(Math.max(this.max.getX(), key.getX()), Math.max(this.max.getY(), key.getY()), Math.max(this.max.getZ(), key.getZ()));
        return true;
    }

    @Override
    public boolean removeBlock(BlockPos pos, boolean movedByPiston) {
        return this.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
    }

    @Override
    public boolean destroyBlock(BlockPos pos, boolean dropResources, @Nullable Entity breaker, int updateLimit) {
        return this.removeBlock(pos, false);
    }

    public Map<BlockPos, BlockState> blocks() {
        return Collections.unmodifiableMap(this.blocks);
    }

    /** Size of the bounding box, or zero when nothing was written. */
    public Vec3i size() {
        if (this.min == null || this.max == null) {
            return Vec3i.ZERO;
        }
        return new Vec3i(this.max.getX() - this.min.getX() + 1, this.max.getY() - this.min.getY() + 1, this.max.getZ() - this.min.getZ() + 1);
    }

    /** Builds the structure template tag. */
    public CompoundTag toTemplateTag() {
        CompoundTag tag = new CompoundTag();
        ListTag palette = new ListTag();
        ListTag blockList = new ListTag();
        List<BlockState> states = new ArrayList<>();
        Map<BlockState, Integer> ids = new LinkedHashMap<>();
        BlockPos origin = this.min == null ? BlockPos.ZERO : this.min;
        for (Map.Entry<BlockPos, BlockState> entry : this.blocks.entrySet()) {
            Integer id = ids.get(entry.getValue());
            if (id == null) {
                id = states.size();
                states.add(entry.getValue());
                ids.put(entry.getValue(), id);
            }
            BlockPos relative = entry.getKey().subtract(origin);
            CompoundTag blockTag = new CompoundTag();
            blockTag.put("pos", intList(relative.getX(), relative.getY(), relative.getZ()));
            blockTag.putInt("state", id);
            blockList.add(blockTag);
        }
        for (BlockState state : states) {
            palette.add(NbtUtils.writeBlockState(state));
        }
        Vec3i size = this.size();
        tag.put("blocks", blockList);
        tag.put("palette", palette);
        tag.put("entities", new ListTag());
        tag.put("size", intList(size.getX(), size.getY(), size.getZ()));
        return NbtUtils.addCurrentDataVersion(tag);
    }

    private static ListTag intList(int... values) {
        ListTag list = new ListTag();
        for (int value : values) {
            list.add(IntTag.valueOf(value));
        }
        return list;
    }

    @Override
    public boolean isStateAtPosition(BlockPos pos, Predicate<BlockState> predicate) {
        return predicate.test(this.blocks.getOrDefault(pos, Blocks.AIR.defaultBlockState()));
    }

    @Override
    public boolean isFluidAtPosition(BlockPos pos, Predicate<FluidState> predicate) {
        return predicate.test(this.blocks.getOrDefault(pos, Blocks.AIR.defaultBlockState()).getFluidState());
    }

    @Override
    public <T extends BlockEntity> Optional<T> getBlockEntity(BlockPos pos, BlockEntityType<T> type) {
        return Optional.empty();
    }

    @Override
    public BlockPos getHeightmapPos(Heightmap.Types type, BlockPos pos) {
        return pos;
    }
}
