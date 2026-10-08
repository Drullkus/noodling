package us.drullk.noodling.datagen;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TexturedModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import us.drullk.noodling.Noodling;

/**
 * Generates block states, block models and item models for the pasta blocks.
 *
 * <p>Each pasta block is a plain {@code cube_all} whose texture is the block's registry name without the
 * {@code _block} suffix (for example {@code noodling:block/tomato_pasta} for {@code tomato_pasta_block}), matching
 * the files in {@code assets/noodling/textures/block}. Item models are not written explicitly: the vanilla
 * {@link ModelProvider} emits a model for every {@code BlockItem} of this mod that points at its block model.
 *
 * <p>Written by Claude Code; conversation transcript: {@code llm/transcripts/transcript_2026-10-08_b2d8f02b-624c-4f11-b4f8-507057a1af26.md}.
 */
public class PastaModelProvider extends ModelProvider {

    private static final String BLOCK_SUFFIX = "_block";

    /** Cube whose six faces all use {@link #pastaTexture(Block)}. */
    private static final TexturedModel.Provider PASTA_CUBE =
            TexturedModel.createDefault(block -> TextureMapping.cube(pastaTexture(block)), ModelTemplates.CUBE_ALL);

    public PastaModelProvider(PackOutput output) {
        super(output, Noodling.MODID);
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        for (var pasta : Noodling.PASTA_BLOCKS) {
            blockModels.createTrivialBlock(pasta.get(), PASTA_CUBE);
        }
    }

    private static Material pastaTexture(Block block) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(block);
        String path = id.getPath();
        if (path.endsWith(BLOCK_SUFFIX)) {
            path = path.substring(0, path.length() - BLOCK_SUFFIX.length());
        }
        return new Material(id.withPath("block/" + path));
    }
}
