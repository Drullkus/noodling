package us.drullk.impastable.commands;

import net.minecraft.commands.Commands;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public class PastaCommands {

    public void registerCommands(RegisterCommandsEvent event) {
        Commands.literal("pasta");
    }

}
