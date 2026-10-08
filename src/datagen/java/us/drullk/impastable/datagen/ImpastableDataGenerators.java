package us.drullk.impastable.datagen;

import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import us.drullk.impastable.Impastable;

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
@EventBusSubscriber(modid = Impastable.MODID, value = Dist.CLIENT)
public final class ImpastableDataGenerators {

    private ImpastableDataGenerators() {
    }

    @SubscribeEvent
    static void onGatherData(GatherDataEvent.Client event) {
        event.createProvider(PastaModelProvider::new);
        event.createProvider(PastaLanguageProvider::new);

        // Loot tables are a reloadable datapack registry, so they go through the registry set builder.
        event.createReloadableRegistryObjects(new RegistrySetBuilder()
                .add(Registries.LOOT_TABLE, new LootTableProvider(Set.of(), List.of(
                        new LootTableProvider.SubProviderEntry(PastaBlockLoot::new, LootContextParamSets.BLOCK)))));
    }
}
