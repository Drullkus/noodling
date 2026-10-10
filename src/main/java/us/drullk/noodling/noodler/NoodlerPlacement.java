package us.drullk.noodling.noodler;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.OptionalLong;

/**
 * Server-side placement for the Noodler: resolves the seed and every parameter from the item's {@link NoodlerState},
 * places the shape at the crosshair hit, and writes the seeds and values that were used back to the item.
 *
 * <p>Written by Claude Code; conversation transcript: {@code llm/transcripts/transcript_2026-10-10_25b84f50-3f88-41a1-a942-98824ffe67dc.md}.
 */
public final class NoodlerPlacement {

    /** Ticks left click must be held before a release places pasta */
    public static final int CHARGE_TICKS = 10;

    private NoodlerPlacement() {
    }

    /** A shape ready to place: the shape, its resolved parameters, and the state the Noodler holds afterwards. */
    public record Prepared(PastaShape shape, ResolvedParams params, NoodlerState nextState) {
    }

    /** Places one piece of pasta for {@code player} and updates the state on {@code stack}. */
    public static void place(ServerPlayer player, ItemStack stack) {
        ServerLevel level = player.level();
        NoodlerState state = stack.getOrDefault(NoodlerComponents.STATE.get(), NoodlerState.DEFAULT);
        Prepared prepared = prepare(state, level.getRandom(), false);

        Vec3 center = NoodlerRaycast.raycast(player, 1.0F).getLocation();
        prepared.shape().place(level, center, prepared.params());
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1.0F, 1.0F);

        stack.set(NoodlerComponents.STATE.get(), prepared.nextState());
    }

    /**
     * Resolves the seed, the shape and every parameter from {@code state}.
     *
     * <p>With {@code keepTypedSeed} false the seed follows the placement rule: a locked seed is used, an unlocked one
     * is re-rolled. With it true (export) the seed text is used whether or not it is locked, and only a blank field
     * rolls, so an export reproduces the shape the Noodler shows and the Noodler keeps showing it afterwards.
     */
    public static Prepared prepare(NoodlerState state, RandomSource random, boolean keepTypedSeed) {
        long seed = keepTypedSeed ? NoodlerSeeds.parse(state.seed()).orElseGet(random::nextLong) : resolveSeed(state, random);
        PastaShape shape = state.shape().map(PastaShapes::byId).orElse(null);
        Map<String, ParamState> nextParams = new LinkedHashMap<>();
        ResolvedParams resolved;
        if (shape == null) {
            shape = PastaShapes.ALL.get(RandomSource.create(seed).nextInt(PastaShapes.ALL.size()));
            resolved = resolve(shape, NoodlerState.DEFAULT, seed, nextParams);
            nextParams = state.params();
        } else {
            resolved = resolve(shape, state, seed, nextParams);
        }
        // Keep the player's own seed text (for example a word) when it was honoured; show the rolled number otherwise.
        boolean typedSeedUsed = NoodlerSeeds.parse(state.seed()).isPresent() && (state.seedLocked() || keepTypedSeed);
        String seedText = typedSeedUsed ? state.seed() : NoodlerSeeds.text(seed);
        return new Prepared(shape, resolved, new NoodlerState(seedText, state.seedLocked(), state.shape(), nextParams));
    }

    /** A locked seed is parsed from the field; otherwise, or when the field is blank, a fresh seed is rolled. */
    static long resolveSeed(NoodlerState state, RandomSource random) {
        if (state.seedLocked()) {
            OptionalLong typed = NoodlerSeeds.parse(state.seed());
            if (typed.isPresent()) {
                return typed.getAsLong();
            }
        }
        return random.nextLong();
    }

    /**
     * Resolves every parameter of {@code shape} and records the state that each one ends up with in
     * {@code nextParams}.
     */
    static ResolvedParams resolve(PastaShape shape, NoodlerState state, long seed, Map<String, ParamState> nextParams) {
        ResolvedParams values = new ResolvedParams();
        int index = 0;
        for (PastaParam param : shape.params()) {
            ParamState paramState = state.param(param.name());
            ParamValue manual = paramState.value().filter(param::accepts).orElse(null);
            ParamState next;
            ParamValue value;
            if (paramState.locked()) {
                OptionalLong typed = NoodlerSeeds.parse(paramState.seed());
                if (typed.isPresent()) {
                    value = param.roll(RandomSource.create(typed.getAsLong()), values);
                    next = paramState.withValue(value);
                } else if (manual != null) {
                    value = manual;
                    next = paramState;
                } else {
                    long derived = NoodlerSeeds.derive(seed, index);
                    value = param.roll(RandomSource.create(derived), values);
                    next = new ParamState(NoodlerSeeds.text(derived), true, java.util.Optional.of(value));
                }
            } else {
                long derived = NoodlerSeeds.derive(seed, index);
                value = param.roll(RandomSource.create(derived), values);
                next = new ParamState(NoodlerSeeds.text(derived), false, java.util.Optional.of(value));
            }
            values.put(param.name(), value);
            nextParams.put(param.name(), next);
            index++;
        }
        return values;
    }

    /** Removes the charge marker from every Noodler the player carries. */
    public static void clearCharges(ServerPlayer player) {
        for (ItemStack stack : player.getInventory()) {
            if (stack.getItem() instanceof NoodlerItem && stack.has(NoodlerComponents.CHARGE.get())) {
                stack.remove(NoodlerComponents.CHARGE.get());
            }
        }
    }

    /** The charge marker on the player's main hand Noodler, if any. */
    public static @Nullable NoodlerCharge mainHandCharge(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        return stack.getItem() instanceof NoodlerItem ? stack.get(NoodlerComponents.CHARGE.get()) : null;
    }
}
