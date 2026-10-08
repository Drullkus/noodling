package us.drullk.impastable.datagen;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.LanguageProvider;
import us.drullk.impastable.Impastable;

import java.util.Locale;

/**
 * Generates {@code assets/impastable/lang/en_us.json}.
 *
 * <p>Block names are derived from the registry name by title-casing each underscore-separated word, so
 * {@code squid_ink_pasta_block} becomes "Squid Ink Pasta Block". The block items share the block's description id
 * (see {@code Item.Properties#useBlockDescriptionPrefix}), so one entry covers both.
 *
 * <p>Written by Claude Code; conversation transcript: {@code llm/transcripts/transcript_2026-10-08_b2d8f02b-624c-4f11-b4f8-507057a1af26.md}.
 */
public class PastaLanguageProvider extends LanguageProvider {

    public PastaLanguageProvider(PackOutput output) {
        super(output, Impastable.MODID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add("itemGroup.impastable", "Im-Pasta-ble");

        for (var pasta : Impastable.PASTA_BLOCKS) {
            Block block = pasta.get();
            add(block, titleCase(BuiltInRegistries.BLOCK.getKey(block).getPath()));
        }
    }

    private static String titleCase(String registryPath) {
        StringBuilder name = new StringBuilder(registryPath.length());
        for (String word : registryPath.split("_")) {
            if (word.isEmpty()) {
                continue;
            }
            if (!name.isEmpty()) {
                name.append(' ');
            }
            name.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1).toLowerCase(Locale.ROOT));
        }
        return name.toString();
    }
}
