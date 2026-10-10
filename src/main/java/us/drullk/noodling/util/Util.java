package us.drullk.noodling.util;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.entity.SignTextSlot;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredBlock;
import org.joml.Quaterniond;
import us.drullk.noodling.Noodling;

import java.util.List;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public class Util {

    private final WeightedList<DeferredBlock<Block>> pastaBlocks = WeightedList.<DeferredBlock<Block>>builder()
            .add(Noodling.TOMATO_PASTA_BLOCK, 3)
            .add(Noodling.CARROT_PASTA_BLOCK, 2)
            .add(Noodling.WHEAT_PASTA_BLOCK, 10)
            .add(Noodling.SPINACH_PASTA_BLOCK, 5)
            .add(Noodling.SPIRULINA_PASTA_BLOCK, 1)
            .add(Noodling.BEETROOT_PASTA_BLOCK, 2)
            .add(Noodling.SQUID_INK_PASTA_BLOCK, 1)
            .build();

    /**
     * One half no sauce (air), one sixth each of the three sauces.
     *
     * <p>Written by Claude Code; conversation transcript: {@code llm/transcripts/transcript_2026-10-10_c3299adc-f781-48a8-9c79-c51c79b26b4b.md}.
     * */
    private final WeightedList<Supplier<BlockState>> sauceBlocks = WeightedList.<Supplier<BlockState>>builder()
            .add(Blocks.AIR::defaultBlockState, 3)
            .add(() -> Noodling.ALFREDO_SAUCE_BLOCK.value().defaultBlockState(), 1)
            .add(() -> Noodling.CHEDDAR_SAUCE_BLOCK.value().defaultBlockState(), 1)
            .add(() -> Noodling.TOMATO_SAUCE_BLOCK.value().defaultBlockState(), 1)
            .build();

    public void placeSignBlock(ServerLevel level, BlockPos pos, List<Component> textList) {
        level.setBlock(pos, Blocks.OAK_SIGN.defaultBlockState(), Block.UPDATE_ALL);

        if (!(level.getBlockEntity(pos) instanceof SignBlockEntity signBlockEntity)) return;

        signBlockEntity.setText(new SignText(
                textList,
                textList,
                DyeColor.BLACK,
                true
        ), SignTextSlot.FRONT);
    }

    public BlockState getRandomPastaBlock(RandomSource random) {
        return this.pastaBlocks.getRandomOrThrow(random).value().defaultBlockState();
    }

    /**
     * A sauce block state to put on top of the pasta, or air for no sauce.
     *
     * <p>Written by Claude Code; conversation transcript: {@code llm/transcripts/transcript_2026-10-10_c3299adc-f781-48a8-9c79-c51c79b26b4b.md}.
     * */
    public BlockState getRandomSauceBlock(RandomSource random) {
        return this.sauceBlocks.getRandomOrThrow(random).get();
    }

    public Quaterniond nextRotation(RandomSource random) {
        return new Quaterniond().rotationXYZ(random.triangle(0, 180f), random.triangle(0, 180f), random.triangle(0, 180f));
    }

}
