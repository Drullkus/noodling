package us.drullk.noodling.client.noodler;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.jspecify.annotations.Nullable;
import us.drullk.noodling.Noodling;
import us.drullk.noodling.noodler.NoodlerCharge;
import us.drullk.noodling.noodler.NoodlerComponents;

/**
 * Gives a charging Noodler the bow's poses: the third-person bow-and-arrow arm pose and the first-person bow draw
 * transform. Both read the charge marker component instead of the vanilla "using item" state, because the charge is
 * driven by left click, which vanilla never ties to item use.
 *
 * <p>The first-person transform is a copy of the vanilla bow branch of the hand renderer, with the time held taken
 * from the charge marker.
 *
 * <p>Written by Claude Code; conversation transcript: {@code llm/transcripts/transcript_2026-10-10_25b84f50-3f88-41a1-a942-98824ffe67dc.md}.
 */
public final class NoodlerClientExtensions implements IClientItemExtensions {

    public static final NoodlerClientExtensions INSTANCE = new NoodlerClientExtensions();

    private NoodlerClientExtensions() {
    }

    public static void register(RegisterClientExtensionsEvent event) {
        event.registerItem(INSTANCE, Noodling.NOODLER.get());
    }

    @Override
    public HumanoidModel.@Nullable ArmPose getArmPose(LivingEntity entityLiving, InteractionHand hand, ItemStack itemStack) {
        return hand == InteractionHand.MAIN_HAND && itemStack.has(NoodlerComponents.CHARGE.get()) ? HumanoidModel.ArmPose.BOW_AND_ARROW : null;
    }

    @Override
    public boolean applyForgeHandTransform(PoseStack poseStack, PlayerRenderState playerRenderState, HumanoidArm arm, ItemStack itemInHand, float partialTick, float equipProcess, float swingProcess) {
        NoodlerCharge charge = itemInHand.get(NoodlerComponents.CHARGE.get());
        ClientLevel level = Minecraft.getInstance().level;
        if (charge == null || level == null) {
            return false;
        }
        int invert = arm == HumanoidArm.RIGHT ? 1 : -1;
        // Vanilla's applyItemArmTransform: equipProcess is the inverse arm height.
        poseStack.translate(invert * 0.56F, -0.52F + equipProcess * -0.6F, -0.72F);

        // Vanilla's BOW branch.
        poseStack.translate(invert * -0.2785682F, 0.18344387F, 0.15731531F);
        poseStack.rotateDegrees(Axis.XP, -13.935F);
        poseStack.rotateDegrees(Axis.YP, invert * 35.3F);
        poseStack.rotateDegrees(Axis.ZP, invert * -9.785F);
        float timeHeld = charge.ticksHeld(level.getGameTime()) + partialTick;
        float power = timeHeld / 20.0F;
        power = (power * power + power * 2.0F) / 3.0F;
        if (power > 1.0F) {
            power = 1.0F;
        }
        if (power > 0.1F) {
            float shakeOffset = Mth.sin((timeHeld - 0.1F) * 1.3F);
            float shake = shakeOffset * (power - 0.1F);
            poseStack.translate(0.0F, shake * 0.004F, 0.0F);
        }
        poseStack.translate(0.0F, 0.0F, power * 0.04F);
        poseStack.scale(1.0F, 1.0F, 1.0F + power * 0.2F);
        poseStack.rotateDegrees(Axis.YN, invert * 45.0F);
        return true;
    }
}
