package us.drullk.noodling.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import us.drullk.noodling.Noodling;
import us.drullk.noodling.pasta.*;
import us.drullk.noodling.util.Util;

public class PastaCommands {

    private final Util helper = new Util();
    private final PennePlacer pennePlacer = new PennePlacer(this.helper);
    private final SpiralPlacer spiralPlacer = new SpiralPlacer(this.helper);
    private final ShellPlacer shellPlacer = new ShellPlacer(this.helper);
    private final RadiatoriPlacer radiatoriPlacer = new RadiatoriPlacer(this.helper);
    private final MacaroniPlacer macaroniPlacer = new MacaroniPlacer(this.helper);
    private final PipettePlacer pipettePlacer = new PipettePlacer(this.helper);

    public void registerCommands(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(Noodling.MODID).requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));

        root.then(Commands.literal("penne").then(Commands.argument("pos", Vec3Argument.vec3(true)).executes(this::penne)));
        root.then(Commands.literal("spiral").then(Commands.argument("pos", Vec3Argument.vec3(true)).executes(this::spiral)));
        root.then(Commands.literal("shell").then(Commands.argument("pos", Vec3Argument.vec3(true)).executes(this::shell)));
        root.then(Commands.literal("radiatori").then(Commands.argument("pos", Vec3Argument.vec3(true)).executes(this::radiatori)));
        root.then(Commands.literal("macaroni").then(Commands.argument("pos", Vec3Argument.vec3(true)).executes(this::macaroni)));
        root.then(Commands.literal("pipette").then(Commands.argument("pos", Vec3Argument.vec3(true)).executes(this::pipette)));

        event.getDispatcher().register(root);
    }

    public int penne(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        Vec3 pos = Vec3Argument.getVec3(ctx, "pos");
        RandomSource random = level.getRandom().forkPositional().at(BlockPos.containing(pos));

        this.pennePlacer.placeRandomPenne(level, pos, random);

        return 0;
    }

    public int spiral(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        Vec3 pos = Vec3Argument.getVec3(ctx, "pos");
        RandomSource random = level.getRandom().forkPositional().at(BlockPos.containing(pos));

        this.spiralPlacer.placeRandomSpiral(level, pos, random);

        return 0;
    }

    public int shell(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        Vec3 pos = Vec3Argument.getVec3(ctx, "pos");
        RandomSource random = level.getRandom().forkPositional().at(BlockPos.containing(pos));

        this.shellPlacer.placeRandomShell(level, pos, random);

        return 0;
    }

    public int radiatori(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        Vec3 pos = Vec3Argument.getVec3(ctx, "pos");
        RandomSource random = level.getRandom().forkPositional().at(BlockPos.containing(pos));

        this.radiatoriPlacer.placeRandomRadiatori(level, pos, random);

        return 0;
    }

    public int macaroni(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        Vec3 pos = Vec3Argument.getVec3(ctx, "pos");
        RandomSource random = level.getRandom().forkPositional().at(BlockPos.containing(pos));

        this.macaroniPlacer.placeRandomMacaroni(level, pos, random);

        return 0;
    }

    public int pipette(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        Vec3 pos = Vec3Argument.getVec3(ctx, "pos");
        RandomSource random = level.getRandom().forkPositional().at(BlockPos.containing(pos));

        this.pipettePlacer.placeRandomPipette(level, pos, random);

        return 0;
    }

}
