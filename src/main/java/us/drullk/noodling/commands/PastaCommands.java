package us.drullk.noodling.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
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
import us.drullk.noodling.noodler.PastaShapeLookup;

@Component
public class PastaCommands {

    private final PastaShapeLookup pastaShapeLookup;

    public PastaCommands(@Autowired PastaShapeLookup pastaShapeLookup) {
        this.pastaShapeLookup = pastaShapeLookup;
    }

    @PostConstruct
    public void registerListener() {
        NeoForge.EVENT_BUS.addListener(this::registerCommands);
    }

    public void registerCommands(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(Noodling.MODID).requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));

        this.pastaShapeLookup.forEach((name, pastaShape) -> root.then(
                Commands.literal(name).then(Commands.argument("pos", Vec3Argument.vec3(true)).executes(this.buildPositionalPastaExecutor(pastaShape)))
        ));

        event.getDispatcher().register(root);
    }

    private Command<CommandSourceStack> buildPositionalPastaExecutor(PastaShape pastaShape) {
        return ctx -> {
            ServerLevel level = ctx.getSource().getLevel();
            Vec3 pos = Vec3Argument.getVec3(ctx, "pos");
            RandomSource random = level.getRandom().forkPositional().at(BlockPos.containing(pos));

            pastaShape.placeRandom(level, pos, random);

            return 0;
        };
    }

}
