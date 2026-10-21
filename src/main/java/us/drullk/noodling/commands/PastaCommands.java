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
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import tamaized.beanification.Autowired;
import tamaized.beanification.Component;
import tamaized.beanification.PostConstruct;
import us.drullk.noodling.Noodling;
import us.drullk.noodling.noodler.PastaShape;
import us.drullk.noodling.pasta.*;

@Component
public class PastaCommands {

    private final PastaShape penneShape;
    private final PastaShape spiralShape;
    private final ShellPlacer shellPlacer;
    private final RadiatoriPlacer radiatoriPlacer;
    private final MacaroniPlacer macaroniPlacer;
    private final PipettePlacer pipettePlacer;

    public PastaCommands(
            @Autowired("penneShape") PastaShape penneShape,
            @Autowired("spiralShape") PastaShape spiralShape,
            @Autowired ShellPlacer shellPlacer,
            @Autowired RadiatoriPlacer radiatoriPlacer,
            @Autowired MacaroniPlacer macaroniPlacer,
            @Autowired PipettePlacer pipettePlacer
    ) {
        this.penneShape = penneShape;
        this.spiralShape = spiralShape;
        this.shellPlacer = shellPlacer;
        this.radiatoriPlacer = radiatoriPlacer;
        this.macaroniPlacer = macaroniPlacer;
        this.pipettePlacer = pipettePlacer;
    }

    @PostConstruct
    public void registerListener() {
        NeoForge.EVENT_BUS.addListener(this::registerCommands);
    }

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

        this.penneShape.placeRandom(level, pos, random);

        return 0;
    }

    public int spiral(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        Vec3 pos = Vec3Argument.getVec3(ctx, "pos");
        RandomSource random = level.getRandom().forkPositional().at(BlockPos.containing(pos));

        this.spiralShape.placeRandom(level, pos, random);

        return 0;
    }

    public int shell(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        Vec3 pos = Vec3Argument.getVec3(ctx, "pos");
        RandomSource random = level.getRandom().forkPositional().at(BlockPos.containing(pos));

        this.shellPlacer.shape().placeRandom(level, pos, random);

        return 0;
    }

    public int radiatori(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        Vec3 pos = Vec3Argument.getVec3(ctx, "pos");
        RandomSource random = level.getRandom().forkPositional().at(BlockPos.containing(pos));

        this.radiatoriPlacer.shape().placeRandom(level, pos, random);

        return 0;
    }

    public int macaroni(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        Vec3 pos = Vec3Argument.getVec3(ctx, "pos");
        RandomSource random = level.getRandom().forkPositional().at(BlockPos.containing(pos));

        this.macaroniPlacer.shape().placeRandom(level, pos, random);

        return 0;
    }

    public int pipette(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        Vec3 pos = Vec3Argument.getVec3(ctx, "pos");
        RandomSource random = level.getRandom().forkPositional().at(BlockPos.containing(pos));

        this.pipettePlacer.shape().placeRandom(level, pos, random);

        return 0;
    }

}
