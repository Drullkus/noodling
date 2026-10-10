package us.drullk.noodling.datagen;

import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredHolder;
import us.drullk.noodling.Noodling;

import java.util.Set;
import java.util.stream.Stream;

/**
 * Block loot tables: every pasta block and every sauce block drops itself, with no tool requirement.
 *
 * <p>Written by Claude Code; conversation transcript: {@code llm/transcripts/transcript_2026-10-08_b2d8f02b-624c-4f11-b4f8-507057a1af26.md}.
 * <p>Written by Claude Code; conversation transcript: {@code llm/transcripts/transcript_2026-10-10_a8ab6d8e-ef9d-4e9b-9ff7-9d0f96002449.md}.
 */
public class PastaBlockLoot extends BlockLootSubProvider {

    public PastaBlockLoot(LootTableSubProvider.Context context) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), context);
    }

    @Override
    protected void generate() {
        for (var pasta : Noodling.PASTA_BLOCKS) {
            dropSelf(pasta.get());
        }
        for (var sauce : Noodling.SAUCE_BLOCKS) {
            dropSelf(sauce.get());
        }
    }

    /** Restricts the "every known block has a loot table" check to this mod's blocks. */
    @Override
    protected Iterable<Block> getKnownBlocks() {
        return Stream.concat(Noodling.PASTA_BLOCKS.stream(), Noodling.SAUCE_BLOCKS.stream()).map(DeferredHolder::get).toList();
    }
}
