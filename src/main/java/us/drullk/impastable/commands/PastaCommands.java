package us.drullk.impastable.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.joml.Quaterniond;
import us.drullk.impastable.Impastable;
import us.drullk.impastable.shape.InnerSpaceIterable;
import us.drullk.impastable.shape.InnerSpacePos;

public class PastaCommands {

    public void registerCommands(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(Impastable.MODID).requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));

        root.then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(this::penne));

        event.getDispatcher().register(root);
    }

    public Holder<Block> getRandomBlock(RandomSource random) {
        return Util.getRandom(Impastable.PASTA_BLOCKS, random);
    }

    private int penne(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        BlockPos centerPos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");

        ServerLevel level = ctx.getSource().getLevel();
        RandomSource random = level.getRandom().forkPositional().at(centerPos);

        BlockState pasta = this.getRandomBlock(random).value().defaultBlockState();

        float diameter = Mth.lerp(random.nextFloat(), 12, 16);
        float radius = diameter * 0.5f;
        float height = Mth.lerp(random.nextFloat(), 50, 80);

        Quaterniond rotation = new Quaterniond().rotationXYZ(random.triangle(0, 180f), random.triangle(0, 180f), random.triangle(0, 180f));

        InnerSpaceIterable spaceIterator = InnerSpaceIterable.centered(new Vec3(centerPos), rotation, diameter, height, diameter);
        for (InnerSpacePos innerSpacePos : spaceIterator) {
            float shapeRadius = Mth.sqrt(innerSpacePos.innerX() * innerSpacePos.innerX() + innerSpacePos.innerZ() * innerSpacePos.innerZ());

            boolean solid = shapeRadius > radius * 0.667f && shapeRadius < radius && 0 > Math.abs(innerSpacePos.innerX() - innerSpacePos.innerY()) * 2 + diameter - height;
            if (solid) level.setBlock(innerSpacePos.toBlockPos(), pasta, Block.UPDATE_ALL);
        }

        return 0;
    }

}
