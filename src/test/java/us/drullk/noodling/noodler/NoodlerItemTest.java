package us.drullk.noodling.noodler;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.KeybindContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.junit.jupiter.api.Test;
import us.drullk.noodling.Noodling;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Checks the Noodler's tooltip names the attack and use key mappings through keybind components.
 *
 * <p>Written by Claude Code; conversation transcript: {@code llm/transcripts/transcript_2026-10-10_25b84f50-3f88-41a1-a942-98824ffe67dc.md}.
 */
class NoodlerItemTest {

    @Test
    void tooltipNamesAttackAndUseKeys() {
        List<Component> lines = new ArrayList<>();
        Noodling.NOODLER.get().appendHoverText(ItemStack.EMPTY, Item.TooltipContext.EMPTY, TooltipDisplay.DEFAULT, lines::add, TooltipFlag.NORMAL);
        assertEquals(2, lines.size());
        assertEquals("key.attack", keybindArgument(lines.get(0), "noodling.tooltip.noodler.place"));
        assertEquals("key.use", keybindArgument(lines.get(1), "noodling.tooltip.noodler.configure"));
    }

    private static String keybindArgument(Component line, String expectedKey) {
        TranslatableContents contents = assertInstanceOf(TranslatableContents.class, line.getContents());
        assertEquals(expectedKey, contents.getKey());
        Component argument = assertInstanceOf(Component.class, contents.getArgs()[0]);
        return assertInstanceOf(KeybindContents.class, argument.getContents()).getName();
    }
}
