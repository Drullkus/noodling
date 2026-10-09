package us.drullk.noodling.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.entity.SignTextSlot;
import us.drullk.noodling.Noodling;

import java.util.List;

@SuppressWarnings("unused")
public class Util {

    public static void placeSignBlock(ServerLevel level, BlockPos pos, List<Component> textList) {
        level.setBlock(pos, Blocks.OAK_SIGN.defaultBlockState(), Block.UPDATE_ALL);

        if (!(level.getBlockEntity(pos) instanceof SignBlockEntity signBlockEntity)) return;

        signBlockEntity.setText(new SignText(
                textList,
                textList,
                DyeColor.BLACK,
                true
        ), SignTextSlot.FRONT);
    }

    public static Holder<Block> getRandomBlock(RandomSource random) {
        return net.minecraft.util.Util.getRandom(Noodling.PASTA_BLOCKS, random);
    }
}
