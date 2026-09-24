package com.mg3dp.birdsineverybiome.mixin;

import java.util.Optional;

import com.mg3dp.birdsineverybiome.ParrotSkins;
import com.mg3dp.birdsineverybiome.PlayerShoulderSkins;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Carries the biome skin of the parrot on each shoulder alongside vanilla's variant int, so the
 * shoulder parrot is drawn with the right texture on every client.
 */
@Mixin(Player.class)
public abstract class PlayerMixin implements PlayerShoulderSkins {
	@Unique
	private static final EntityDataAccessor<Integer> BIOMEPARROTS_SHOULDER_LEFT =
			SynchedEntityData.defineId(Player.class, EntityDataSerializers.INT);

	@Unique
	private static final EntityDataAccessor<Integer> BIOMEPARROTS_SHOULDER_RIGHT =
			SynchedEntityData.defineId(Player.class, EntityDataSerializers.INT);

	/** Server side only: the skin of the parrot being handed over, used by whichever side takes it. */
	@Unique
	private int birdsineverybiome$pendingShoulderSkin = ParrotSkins.VANILLA;

	@Inject(method = "defineSynchedData", at = @At("TAIL"))
	private void birdsineverybiome$defineShoulderSkins(SynchedEntityData.Builder builder, CallbackInfo ci) {
		builder.define(BIOMEPARROTS_SHOULDER_LEFT, ParrotSkins.VANILLA);
		builder.define(BIOMEPARROTS_SHOULDER_RIGHT, ParrotSkins.VANILLA);
	}

	@Inject(method = "setShoulderParrotLeft", at = @At("TAIL"))
	private void birdsineverybiome$leftShoulderSkin(Optional<Parrot.Variant> variant, CallbackInfo ci) {
		if (variant.isPresent()) {
			this.birdsineverybiome$setShoulderSkin(true, this.birdsineverybiome$pendingShoulderSkin);
		}
	}

	@Inject(method = "setShoulderParrotRight", at = @At("TAIL"))
	private void birdsineverybiome$rightShoulderSkin(Optional<Parrot.Variant> variant, CallbackInfo ci) {
		if (variant.isPresent()) {
			this.birdsineverybiome$setShoulderSkin(false, this.birdsineverybiome$pendingShoulderSkin);
		}
	}

	@Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
	private void birdsineverybiome$saveShoulderSkins(ValueOutput output, CallbackInfo ci) {
		output.putInt("BirdsInEveryBiomeShoulderLeft", this.birdsineverybiome$shoulderSkin(true));
		output.putInt("BirdsInEveryBiomeShoulderRight", this.birdsineverybiome$shoulderSkin(false));
	}

	@Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
	private void birdsineverybiome$loadShoulderSkins(ValueInput input, CallbackInfo ci) {
		this.birdsineverybiome$setShoulderSkin(true, input.getIntOr("BirdsInEveryBiomeShoulderLeft", ParrotSkins.VANILLA));
		this.birdsineverybiome$setShoulderSkin(false, input.getIntOr("BirdsInEveryBiomeShoulderRight", ParrotSkins.VANILLA));
	}

	@Override
	public int birdsineverybiome$shoulderSkin(boolean left) {
		Player self = (Player) (Object) this;
		return self.getEntityData().get(left ? BIOMEPARROTS_SHOULDER_LEFT : BIOMEPARROTS_SHOULDER_RIGHT);
	}

	@Override
	public void birdsineverybiome$setShoulderSkin(boolean left, int skin) {
		Player self = (Player) (Object) this;
		self.getEntityData().set(left ? BIOMEPARROTS_SHOULDER_LEFT : BIOMEPARROTS_SHOULDER_RIGHT, skin);
	}

	@Override
	public void birdsineverybiome$setPendingShoulderSkin(int skin) {
		this.birdsineverybiome$pendingShoulderSkin = skin;
	}
}
