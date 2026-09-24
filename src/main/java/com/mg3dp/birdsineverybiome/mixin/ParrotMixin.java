package com.mg3dp.birdsineverybiome.mixin;

import com.mg3dp.birdsineverybiome.ParrotSkinAccess;
import com.mg3dp.birdsineverybiome.ParrotSkins;
import com.mg3dp.birdsineverybiome.ParrotSpawnRules;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Parrot.class)
public abstract class ParrotMixin implements ParrotSkinAccess {
	@Unique
	private static final EntityDataAccessor<Integer> BIOMEPARROTS_SKIN =
			SynchedEntityData.defineId(Parrot.class, EntityDataSerializers.INT);

	@Unique
	private static final String BIOMEPARROTS_NBT_KEY = "BirdsInEveryBiomeSkin";

	/** Server-side only: true once this parrot has been given a skin. */
	@Unique
	private boolean birdsineverybiome$skinAssigned = false;

	@Inject(method = "defineSynchedData", at = @At("TAIL"))
	private void birdsineverybiome$defineSkinData(SynchedEntityData.Builder builder, CallbackInfo ci) {
		builder.define(BIOMEPARROTS_SKIN, ParrotSkins.VANILLA);
	}

	// ---------------------------------------------------------------- spawning
	/** Replaces vanilla's grass/leaves/logs-only spawn check. */
	@Inject(method = "checkParrotSpawnRules", at = @At("HEAD"), cancellable = true)
	private static void birdsineverybiome$relaxedSpawnRules(EntityType<Parrot> type, LevelAccessor level,
			EntitySpawnReason reason, BlockPos pos, RandomSource random, CallbackInfoReturnable<Boolean> cir) {
		cir.setReturnValue(ParrotSpawnRules.canSpawn(type, level, reason, pos, random));
	}

	// ---------------------------------------------------------------- skin assignment
	/** Natural spawns, spawn eggs, /summon ... anything that goes through finalizeSpawn. */
	@Inject(method = "finalizeSpawn", at = @At("TAIL"))
	private void birdsineverybiome$assignSkinOnSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
			EntitySpawnReason reason, SpawnGroupData groupData, CallbackInfoReturnable<SpawnGroupData> cir) {
		this.birdsineverybiome$assignSkin();
	}

	/** Safety net for parrots that never went through finalizeSpawn (e.g. chunk generation). */
	@Inject(method = "aiStep", at = @At("HEAD"))
	private void birdsineverybiome$assignSkinOnTick(CallbackInfo ci) {
		this.birdsineverybiome$assignSkin();
	}

	@Unique
	private void birdsineverybiome$assignSkin() {
		if (this.birdsineverybiome$skinAssigned) {
			return;
		}

		Parrot self = (Parrot) (Object) this;

		// Clients only get the value through the data tracker, never assign one there.
		if (self.level().isClientSide()) {
			return;
		}

		this.birdsineverybiome$skinAssigned = true;
		String skin = ParrotSkins.skinFor(self.level().getBiome(self.blockPosition()));
		this.birdsineverybiome$setSkin(ParrotSkins.indexOf(skin));
	}

	// ---------------------------------------------------------------- saving
	@Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
	private void birdsineverybiome$saveSkin(ValueOutput output, CallbackInfo ci) {
		if (this.birdsineverybiome$skinAssigned) {
			output.putString(BIOMEPARROTS_NBT_KEY, ParrotSkins.skinName(this.birdsineverybiome$skin()));
		}
	}

	@Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
	private void birdsineverybiome$loadSkin(ValueInput input, CallbackInfo ci) {
		// Only adopt a skin that was actually saved. A parrot with no saved skin (e.g. from a world
		// that predates the mod) stays unassigned and gets one on its first tick, once its position
		// is final - NBT can be read before a summoned entity has been placed, and the biome at
		// (0,0,0) is not the biome it ends up in.
		String saved = input.getStringOr(BIOMEPARROTS_NBT_KEY, "");

		if (!saved.isEmpty()) {
			this.birdsineverybiome$skinAssigned = true;
			this.birdsineverybiome$setSkin(ParrotSkins.indexOf(saved));
		}
	}

	// ---------------------------------------------------------------- duck interface
	@Override
	public int birdsineverybiome$skin() {
		return ((Parrot) (Object) this).getEntityData().get(BIOMEPARROTS_SKIN);
	}

	@Override
	public void birdsineverybiome$setSkin(int index) {
		((Parrot) (Object) this).getEntityData().set(BIOMEPARROTS_SKIN, index);
	}
}
