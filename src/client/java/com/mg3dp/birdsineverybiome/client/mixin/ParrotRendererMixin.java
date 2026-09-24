package com.mg3dp.birdsineverybiome.client.mixin;

import com.mg3dp.birdsineverybiome.ParrotSkinAccess;
import com.mg3dp.birdsineverybiome.ParrotSkins;
import com.mg3dp.birdsineverybiome.client.ParrotSkinRenderState;

import net.minecraft.client.renderer.entity.ParrotRenderer;
import net.minecraft.client.renderer.entity.state.ParrotRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.parrot.Parrot;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParrotRenderer.class)
public abstract class ParrotRendererMixin {
	/** Copy the parrot's synced skin into the render state (runs every frame, client side only). */
	@Inject(
			method = "extractRenderState(Lnet/minecraft/world/entity/animal/parrot/Parrot;Lnet/minecraft/client/renderer/entity/state/ParrotRenderState;F)V",
			at = @At("TAIL"))
	private void birdsineverybiome$copySkin(Parrot parrot, ParrotRenderState state, float partialTick, CallbackInfo ci) {
		((ParrotSkinRenderState) state).birdsineverybiome$setSkin(((ParrotSkinAccess) parrot).birdsineverybiome$skin());
	}

	/** Use the biome texture when the parrot has one, otherwise fall through to the vanilla variant texture. */
	@Inject(
			method = "getTextureLocation(Lnet/minecraft/client/renderer/entity/state/ParrotRenderState;)Lnet/minecraft/resources/Identifier;",
			at = @At("HEAD"),
			cancellable = true)
	private void birdsineverybiome$biomeTexture(ParrotRenderState state, CallbackInfoReturnable<Identifier> cir) {
		int skin = ((ParrotSkinRenderState) state).birdsineverybiome$skin();

		if (skin > ParrotSkins.VANILLA) {
			cir.setReturnValue(ParrotSkins.texture(skin));
		}
	}
}
