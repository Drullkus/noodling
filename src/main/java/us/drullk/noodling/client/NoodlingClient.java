package us.drullk.noodling.client;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import us.drullk.noodling.Noodling;
import us.drullk.noodling.client.imgui.ImGuiRenderer;
import us.drullk.noodling.client.noodler.NoodlerChargeTracker;
import us.drullk.noodling.client.noodler.NoodlerClientExtensions;
import us.drullk.noodling.client.noodler.NoodlerScreen;
import us.drullk.noodling.noodler.NoodlerItem;

/**
 * Client entry point: wires the ImGui renderer, the Noodler screen, the charge tracker and the item's client
 * extensions. This class only loads on the physical client.
 *
 * <p>Written by Claude Code; conversation transcript: {@code llm/transcripts/transcript_2026-10-10_25b84f50-3f88-41a1-a942-98824ffe67dc.md}.
 */
@Mod(value = Noodling.MODID, dist = Dist.CLIENT)
public final class NoodlingClient {

    public NoodlingClient(IEventBus modEventBus) {
        modEventBus.addListener(ImGuiRenderer::registerPipeline);
        modEventBus.addListener(NoodlerClientExtensions::register);

        NeoForge.EVENT_BUS.addListener(ImGuiRenderer::onRenderFrame);
        NeoForge.EVENT_BUS.addListener(NoodlerChargeTracker::onClientTick);
        NeoForge.EVENT_BUS.addListener(NoodlerChargeTracker::onInteractionKey);
        NeoForge.EVENT_BUS.addListener(NoodlerChargeTracker::onComputeFov);

        NoodlerItem.screenOpener = hand -> Minecraft.getInstance().gui.setScreen(new NoodlerScreen(hand));
    }
}
