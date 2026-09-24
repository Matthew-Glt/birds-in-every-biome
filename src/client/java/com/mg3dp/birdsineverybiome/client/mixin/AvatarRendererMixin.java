package com.mg3dp.birdsineverybiome.client.mixin;

import com.mg3dp.birdsineverybiome.PlayerShoulderSkins;
import com.mg3dp.birdsineverybiome.client.AvatarSkinRenderState;

import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Copies the synced shoulder skins onto the avatar render state, next to vanilla's parrot variants. */
@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {
	@Inject(
			method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
			at = @At("TAIL"))
	private void birdsineverybiome$copyShoulderSkins(Avatar avatar, AvatarRenderState state, float partialTick, CallbackInfo ci) {
		if (avatar instanceof Player player) {
			PlayerShoulderSkins skins = (PlayerShoulderSkins) player;
			AvatarSkinRenderState renderState = (AvatarSkinRenderState) state;
			renderState.birdsineverybiome$setShoulderSkin(true, skins.birdsineverybiome$shoulderSkin(true));
			renderState.birdsineverybiome$setShoulderSkin(false, skins.birdsineverybiome$shoulderSkin(false));
		}
	}
}
