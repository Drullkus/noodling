package us.drullk.noodling.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import us.drullk.noodling.Noodling;
import us.drullk.noodling.pasta.PastaPlacers;

public class PastaCommands {

    private final PastaPlacers pastaPlacers;

    public PastaCommands(PastaPlacers pastaPlacers) {
        this.pastaPlacers = pastaPlacers;
    }

    public void registerCommands(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(Noodling.MODID).requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));

        root.then(Commands.literal("penne").then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(this::penne)));
        root.then(Commands.literal("spiral").then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(this::spiral)));
        root.then(Commands.literal("shell").then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(this::shell)));

        event.getDispatcher().register(root);
    }

    public int penne(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        this.pastaPlacers.placePenne(ctx.getSource().getLevel(), BlockPosArgument.getLoadedBlockPos(ctx, "pos"));

        return 0;
    }

    public int spiral(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        this.pastaPlacers.placeSpiral(ctx.getSource().getLevel(), BlockPosArgument.getLoadedBlockPos(ctx, "pos"));

        return 0;
    }

    public int shell(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        this.pastaPlacers.placeShell(ctx.getSource().getLevel(), BlockPosArgument.getLoadedBlockPos(ctx, "pos"));

        return 0;
    }

}
