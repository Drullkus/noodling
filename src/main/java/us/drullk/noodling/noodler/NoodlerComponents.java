package us.drullk.noodling.noodler;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import us.drullk.noodling.Noodling;

/**
 * Data components stored on the Noodler item.
 *
 * <p>Written by Claude Code; conversation transcript: {@code llm/transcripts/transcript_2026-10-10_25b84f50-3f88-41a1-a942-98824ffe67dc.md}.
 */
public final class NoodlerComponents {

    public static final DeferredRegister.DataComponents REGISTER = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Noodling.MODID);

    /** The GUI state: seed, lock, selected shape and per-parameter states. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<NoodlerState>> STATE = REGISTER.registerComponentType("noodler_state",
            builder -> builder.persistent(NoodlerState.CODEC).networkSynchronized(NoodlerState.STREAM_CODEC));

    /** Present only while left click is held. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<NoodlerCharge>> CHARGE = REGISTER.registerComponentType("noodler_charge",
            builder -> builder.persistent(NoodlerCharge.CODEC).networkSynchronized(NoodlerCharge.STREAM_CODEC));

    private NoodlerComponents() {
    }
}
