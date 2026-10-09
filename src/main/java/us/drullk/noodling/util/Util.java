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

    public Quaterniond nextRotation(RandomSource random) {
        return new Quaterniond().rotationXYZ(random.triangle(0, 180f), random.triangle(0, 180f), random.triangle(0, 180f));
    }

}
