package us.drullk.noodling.datagen;

import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import us.drullk.noodling.Noodling;

import java.util.List;
import java.util.Set;

/**
 * Registers every data provider for the mod when {@code ./gradlew runData} fires {@link GatherDataEvent.Client}.
 *
 * <p>Client and server resources are both generated in the single {@code clientData} run, matching what NeoForge
 * itself does, so there is only one generated resources folder to commit.
 *
 * <p>Written by Claude Code; conversation transcript: {@code llm/transcripts/transcript_2026-10-08_b2d8f02b-624c-4f11-b4f8-507057a1af26.md}.
 */
@Mod(Noodling.MODID)
public final class PastaDataGenerators {

    private static final RegistrySetBuilder DATAPACK_BOOTSTRAPS = new RegistrySetBuilder()
            .add(Registries.LOOT_TABLE, new LootTableProvider(Set.of(), List.of(new LootTableProvider.SubProviderEntry(PastaBlockLoot::new, LootContextParamSets.BLOCK))));

    public PastaDataGenerators(IEventBus modEventBus) {
        modEventBus.addListener(this::onGatherData);
    }

    private void onGatherData(GatherDataEvent.Client event) {
        event.createProvider(PastaModelProvider::new);
        event.createProvider(PastaLanguageProvider::new);

        event.createReloadableRegistryObjects(DATAPACK_BOOTSTRAPS);
    }
}
