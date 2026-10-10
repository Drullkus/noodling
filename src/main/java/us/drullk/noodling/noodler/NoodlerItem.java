package us.drullk.noodling.noodler;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;

import java.util.function.Consumer;

/**
 * The Noodler places random pasta shapes where the player looks.
 *
 * <p>Right click (not sneaking) opens the ImGui settings screen on the client. Holding left click for
 * {@link NoodlerPlacement#CHARGE_TICKS} ticks and releasing places a shape; that path is driven by client events and
 * packets rather than by this class, see the client charge tracker and {@code NoodlerPayloads}. Only the main hand
 * is active. The item cannot stack, has no durability and no recipe.
 *
 * <p>Written by Claude Code; conversation transcript: {@code llm/transcripts/transcript_2026-10-10_25b84f50-3f88-41a1-a942-98824ffe67dc.md}.
 */
public class NoodlerItem extends Item {

    /** Set by the client entry point to open the settings screen; a no-op on the dedicated server. */
    public static Consumer<InteractionHand> screenOpener = hand -> {
    };

    public NoodlerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        return this.use(context.getLevel(), player, context.getHand());
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            screenOpener.accept(hand);
        }
        return InteractionResult.CONSUME;
    }

    /**
     * Two tooltip lines that name the keys the Noodler listens to. The keybind components resolve to whatever the
     * player bound to attack and use, the same mappings the client charge tracker and screen opener check.
     */
    @Override
    public void appendHoverText(ItemStack itemStack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag tooltipFlag) {
        builder.accept(Component.translatable("noodling.tooltip.noodler.place", Component.keybind("key.attack")).withStyle(ChatFormatting.GRAY));
        builder.accept(Component.translatable("noodling.tooltip.noodler.configure", Component.keybind("key.use")).withStyle(ChatFormatting.GRAY));
    }

    /** Sneak + right click reaches the block instead of the GUI, so chests and doors still work. */
    @Override
    public boolean doesSneakBypassUse(ItemStack stack, LevelReader level, BlockPos pos, Player player) {
        return true;
    }

    /** The charge marker changes the stack's components; that must not replay the equip animation. */
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }
}
