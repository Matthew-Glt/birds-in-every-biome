package com.mg3dp.birdsineverybiome.client.mixin;

import com.mg3dp.birdsineverybiome.ParrotSkins;
import com.mg3dp.birdsineverybiome.client.AvatarSkinRenderState;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ParrotRenderer;
import net.minecraft.client.renderer.entity.layers.ParrotOnShoulderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.parrot.Parrot;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The shoulder parrot is drawn from the avatar's render state, which is where the biome skin is
 * copied to. Vanilla resolves the texture with a static ParrotRenderer.getVariantTexture call, so
 * that call is redirected while the parrot's shoulder skin is stashed here.
 */
@Mixin(ParrotOnShoulderLayer.class)
public abstract class ParrotOnShoulderLayerMixin {
	@Unique
	private static final String BIOMEPARROTS_SUBMIT_ON_SHOULDER =
			"submitOnShoulder(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lnet/minecraft/world/entity/animal/parrot/Parrot$Variant;FFZ)V";

	/** Render-thread only: the skin of the parrot currently being submitted. */
	@Unique
	private static int birdsineverybiome$currentShoulderSkin = ParrotSkins.VANILLA;

	@Inject(method = BIOMEPARROTS_SUBMIT_ON_SHOULDER, at = @At("HEAD"))
	private void birdsineverybiome$pickShoulderSkin(PoseStack poseStack, SubmitNodeCollector collector, int light,
			AvatarRenderState state, Parrot.Variant variant, float xRot, float yRot, boolean left, CallbackInfo ci) {
		birdsineverybiome$currentShoulderSkin = ((AvatarSkinRenderState) state).birdsineverybiome$shoulderSkin(left);
	}

	@Inject(method = BIOMEPARROTS_SUBMIT_ON_SHOULDER, at = @At("RETURN"))
	private void birdsineverybiome$clearShoulderSkin(PoseStack poseStack, SubmitNodeCollector collector, int light,
			AvatarRenderState state, Parrot.Variant variant, float xRot, float yRot, boolean left, CallbackInfo ci) {
		birdsineverybiome$currentShoulderSkin = ParrotSkins.VANILLA;
	}

	@Redirect(
			method = BIOMEPARROTS_SUBMIT_ON_SHOULDER,
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/renderer/entity/ParrotRenderer;getVariantTexture(Lnet/minecraft/world/entity/animal/parrot/Parrot$Variant;)Lnet/minecraft/resources/Identifier;"))
	private Identifier birdsineverybiome$shoulderTexture(Parrot.Variant variant) {
		int skin = birdsineverybiome$currentShoulderSkin;
		return skin > ParrotSkins.VANILLA ? ParrotSkins.texture(skin) : ParrotRenderer.getVariantTexture(variant);
	}
}
