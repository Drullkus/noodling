package us.drullk.noodling.datagen;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.LanguageProvider;
import us.drullk.noodling.Noodling;
import us.drullk.noodling.noodler.PastaParam;
import us.drullk.noodling.noodler.PastaShape;
import us.drullk.noodling.noodler.PastaShapes;

import java.util.LinkedHashSet;

import java.util.Locale;

/**
 * Generates {@code assets/noodling/lang/en_us.json}.
 *
 * <p>Pasta and sauce block names are derived from the registry name by title-casing each underscore-separated word, so
 * {@code squid_ink_pasta_block} becomes "Squid Ink Pasta Block". The block items share the block's description id
 * (see {@code Item.Properties#useBlockDescriptionPrefix}), so one entry covers both.
 *
 * <p>The Noodler's item name, its GUI labels, the pasta shape names and the shape parameter names are listed here
 * too; parameter names are derived from the parameter keys the same way as block names.
 *
 * <p>Written by Claude Code; conversation transcript: {@code llm/transcripts/transcript_2026-10-08_b2d8f02b-624c-4f11-b4f8-507057a1af26.md}.
 * <p>Written by Claude Code; conversation transcript: {@code llm/transcripts/transcript_2026-10-10_25b84f50-3f88-41a1-a942-98824ffe67dc.md}.
 * <p>Written by Claude Code; conversation transcript: {@code llm/transcripts/transcript_2026-10-10_a8ab6d8e-ef9d-4e9b-9ff7-9d0f96002449.md}.
 * <p>Written by Claude Code; conversation transcript: {@code llm/transcripts/transcript_2026-10-10_c3299adc-f781-48a8-9c79-c51c79b26b4b.md}.
 */
public class PastaLanguageProvider extends LanguageProvider {

    public PastaLanguageProvider(PackOutput output) {
        super(output, Noodling.MODID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add("itemGroup." + Noodling.MODID, "Noodling");

        for (var pasta : Noodling.PASTA_BLOCKS) {
            addBlockName(pasta.get());
        }
        for (var sauce : Noodling.SAUCE_BLOCKS) {
            addBlockName(sauce.get());
        }

        add(Noodling.NOODLER.get(), "Noodler");
        add("noodling.gui.title", "Noodler");
        add("noodling.gui.seed", "Seed");
        add("noodling.gui.lock_seed", "Lock seed");
        add("noodling.gui.pasta", "Pasta");
        add("noodling.gui.no_sauce", "No sauce");
        add("noodling.gui.param_seed", "Seed");
        add("noodling.gui.param_lock", "Lock");
        add("noodling.gui.param_value", "Value");
        add("noodling.gui.axis", "Axis");
        add("noodling.gui.radians", "Radians");
        add("noodling.shape.random", "Random pasta");
        add("noodling.gui.export", "Export .nbt...");
        add("noodling.tooltip.noodler.place", "Hold %s to place Pasta");
        add("noodling.tooltip.noodler.configure", "Click %s to configure pasta generation");
        add("noodling.export.choosing", "Choose where to save the structure template");
        add("noodling.export.requesting", "Building the structure template...");
        add("noodling.export.saved", "Saved structure template to %s");
        add("noodling.export.cancelled", "Export cancelled");
        add("noodling.export.failed", "Export failed: %s");
        add("noodling.export.too_large", "Export failed: the shape is too large to send");
        add("noodling.export.no_noodler", "Export failed: hold the Noodler in your main hand");

        LinkedHashSet<String> paramNames = new LinkedHashSet<>();
        for (PastaShape shape : PastaShapes.ALL) {
            add(shape.descriptionId(), titleCase(shape.id()));
            for (PastaParam param : shape.params()) {
                if (paramNames.add(param.name())) {
                    add(param.descriptionId(), titleCase(param.name()));
                }
            }
        }
    }

    private void addBlockName(Block block) {
        add(block, titleCase(BuiltInRegistries.BLOCK.getKey(block).getPath()));
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
