package com.mg3dp.birdsineverybiome.mixin;

import com.mg3dp.birdsineverybiome.ParrotSkinAccess;
import com.mg3dp.birdsineverybiome.PlayerShoulderSkins;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.animal.parrot.ShoulderRidingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The parrot is deleted from the world once it hops onto a shoulder, and only its variant reaches
 * clients. Stash the parrot's biome skin on the player first so the shoulder renders correctly.
 */
@Mixin(ShoulderRidingEntity.class)
public abstract class ShoulderRidingEntityMixin {
	@Inject(method = "setEntityOnShoulder", at = @At("HEAD"))
	private void birdsineverybiome$stashShoulderSkin(ServerPlayer player, CallbackInfoReturnable<Boolean> cir) {
		if ((Object) this instanceof Parrot parrot) {
			((PlayerShoulderSkins) player).birdsineverybiome$setPendingShoulderSkin(((ParrotSkinAccess) parrot).birdsineverybiome$skin());
		}
	}
}
