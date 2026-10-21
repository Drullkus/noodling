package us.drullk.noodling;

import org.slf4j.Logger;
import tamaized.beanification.BeanContext;

import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import us.drullk.noodling.network.NoodlerPayloads;
import us.drullk.noodling.noodler.NoodlerComponents;
import us.drullk.noodling.noodler.NoodlerItem;

import java.util.List;

@Mod(Noodling.MODID)
public class Noodling {

    public static final String MODID = "noodling";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredBlock<Block> TOMATO_PASTA_BLOCK = BLOCKS.registerSimpleBlock("tomato_pasta_block", p -> p.mapColor(MapColor.TERRACOTTA_RED).strength(0.8F).ignitedByLava());
    public static final DeferredBlock<Block> CARROT_PASTA_BLOCK = BLOCKS.registerSimpleBlock("carrot_pasta_block", p -> p.mapColor(MapColor.TERRACOTTA_ORANGE).strength(0.8F).ignitedByLava());
    public static final DeferredBlock<Block> WHEAT_PASTA_BLOCK = BLOCKS.registerSimpleBlock("wheat_pasta_block", p -> p.mapColor(MapColor.TERRACOTTA_YELLOW).strength(0.8F).ignitedByLava());
    public static final DeferredBlock<Block> SPINACH_PASTA_BLOCK = BLOCKS.registerSimpleBlock("spinach_pasta_block", p -> p.mapColor(MapColor.TERRACOTTA_GREEN).strength(0.8F).ignitedByLava());
    public static final DeferredBlock<Block> SPIRULINA_PASTA_BLOCK = BLOCKS.registerSimpleBlock("spirulina_pasta_block", p -> p.mapColor(MapColor.TERRACOTTA_BLUE).strength(0.8F).ignitedByLava());
    public static final DeferredBlock<Block> BEETROOT_PASTA_BLOCK = BLOCKS.registerSimpleBlock("beetroot_pasta_block", p -> p.mapColor(MapColor.TERRACOTTA_MAGENTA).strength(0.8F).ignitedByLava());
    public static final DeferredBlock<Block> SQUID_INK_PASTA_BLOCK = BLOCKS.registerSimpleBlock("squid_ink_pasta_block", p -> p.mapColor(MapColor.TERRACOTTA_BLACK).strength(0.8F).ignitedByLava());

    public static final DeferredBlock<Block> ALFREDO_SAUCE_BLOCK = BLOCKS.registerSimpleBlock("alfredo_sauce_block", p -> p.mapColor(MapColor.TERRACOTTA_WHITE).strength(0.8F));
    public static final DeferredBlock<Block> CHEDDAR_SAUCE_BLOCK = BLOCKS.registerSimpleBlock("cheddar_sauce_block", p -> p.mapColor(MapColor.TERRACOTTA_YELLOW).strength(0.8F));
    public static final DeferredBlock<Block> TOMATO_SAUCE_BLOCK = BLOCKS.registerSimpleBlock("tomato_sauce_block", p -> p.mapColor(MapColor.TERRACOTTA_RED).strength(0.8F));

    public static final List<DeferredBlock<Block>> PASTA_BLOCKS = List.of(TOMATO_PASTA_BLOCK, CARROT_PASTA_BLOCK, WHEAT_PASTA_BLOCK, SPINACH_PASTA_BLOCK, SPIRULINA_PASTA_BLOCK, BEETROOT_PASTA_BLOCK, SQUID_INK_PASTA_BLOCK);
    public static final List<DeferredBlock<Block>> SAUCE_BLOCKS = List.of(ALFREDO_SAUCE_BLOCK, CHEDDAR_SAUCE_BLOCK, TOMATO_SAUCE_BLOCK);

    public static final DeferredItem<BlockItem> TOMATO_PASTA_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("tomato_pasta_block", TOMATO_PASTA_BLOCK);
    public static final DeferredItem<BlockItem> CARROT_PASTA_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("carrot_pasta_block", CARROT_PASTA_BLOCK);
    public static final DeferredItem<BlockItem> WHEAT_PASTA_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("wheat_pasta_block", WHEAT_PASTA_BLOCK);
    public static final DeferredItem<BlockItem> SPINACH_PASTA_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("spinach_pasta_block", SPINACH_PASTA_BLOCK);
    public static final DeferredItem<BlockItem> SPIRULINA_PASTA_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("spirulina_pasta_block", SPIRULINA_PASTA_BLOCK);
    public static final DeferredItem<BlockItem> BEETROOT_PASTA_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("beetroot_pasta_block", BEETROOT_PASTA_BLOCK);
    public static final DeferredItem<BlockItem> SQUID_INK_PASTA_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("squid_ink_pasta_block", SQUID_INK_PASTA_BLOCK);

    public static final DeferredItem<BlockItem> ALFREDO_SAUCE_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("alfredo_sauce_block", ALFREDO_SAUCE_BLOCK);
    public static final DeferredItem<BlockItem> CHEDDAR_SAUCE_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("cheddar_sauce_block", CHEDDAR_SAUCE_BLOCK);
    public static final DeferredItem<BlockItem> TOMATO_SAUCE_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("tomato_sauce_block", TOMATO_SAUCE_BLOCK);

    /**
     * The Noodler places random pasta where the player looks. It does not stack, has Epic rarity, no recipe and no
     * durability. Its settings live in {@link NoodlerComponents}.
     *
     * <p>Written by Claude Code; conversation transcript: {@code llm/transcripts/transcript_2026-10-10_25b84f50-3f88-41a1-a942-98824ffe67dc.md}.
     */
    public static final DeferredItem<NoodlerItem> NOODLER = ITEMS.registerItem("noodler", NoodlerItem::new, p -> p.stacksTo(1).rarity(Rarity.EPIC));

    //public static final DeferredItem<Item> EXAMPLE_ITEM = ITEMS.registerSimpleItem("example_item", p -> p.food(new FoodProperties.Builder()
    //        .alwaysEdible().nutrition(1).saturationModifier(2f).build()));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> PASTA_TAB = CREATIVE_MODE_TABS.register("pasta_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup." + Noodling.MODID))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(WHEAT_PASTA_BLOCK::toStack)
            .displayItems((parameters, output) -> {
                output.accept(NOODLER.get()); // First slot; registration added by Claude Code, see NOODLER.
                output.accept(WHEAT_PASTA_BLOCK.get());
                output.accept(TOMATO_PASTA_BLOCK.get());
                output.accept(CARROT_PASTA_BLOCK.get());
                output.accept(SPINACH_PASTA_BLOCK.get());
                output.accept(SPIRULINA_PASTA_BLOCK.get());
                output.accept(BEETROOT_PASTA_BLOCK.get());
                output.accept(SQUID_INK_PASTA_BLOCK.get());
                output.accept(ALFREDO_SAUCE_BLOCK.get());
                output.accept(CHEDDAR_SAUCE_BLOCK.get());
                output.accept(TOMATO_SAUCE_BLOCK.get());
            }).build());

    /**
     * Registers the deferred registers, the Noodler's data components and payloads, and the commands.
     *
     * <p>Noodler registration written by Claude Code; conversation transcript: {@code llm/transcripts/transcript_2026-10-10_25b84f50-3f88-41a1-a942-98824ffe67dc.md}.
     */
    public Noodling(IEventBus modEventBus, ModContainer modContainer) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        NoodlerComponents.REGISTER.register(modEventBus);
        modEventBus.addListener(NoodlerPayloads::register);
    }

    static {
        BeanContext.init(MODID);
    }

}
