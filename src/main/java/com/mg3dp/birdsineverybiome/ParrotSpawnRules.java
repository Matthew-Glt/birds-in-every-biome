package com.mg3dp.birdsineverybiome;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

/**
 * Vanilla only lets parrots spawn on grass, leaves or logs, which would leave sand, snow,
 * stone and netherrack barren. This is the relaxed version: any solid floor works.
 */
public final class ParrotSpawnRules {
	private ParrotSpawnRules() {}

	/** Why a position was accepted or rejected - printed by /birds. */
	public enum Denial {
		OK("ok"),
		WRONG_REASON("not a natural spawn"),
		NO_FLOOR("no solid block below"),
		NO_SPACE("block in the way"),
		FLUID("inside a fluid"),
		TOO_DARK("too dark (needs light > 8 in the Overworld)");

		private final String message;

		Denial(String message) {
			this.message = message;
		}

		public String message() {
			return this.message;
		}
	}

	public static Denial check(EntityType<Parrot> type, LevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
		// Spawn eggs, /summon, spawners and breeding are never blocked.
		if (reason != EntitySpawnReason.NATURAL && reason != EntitySpawnReason.CHUNK_GENERATION) {
			return Denial.OK;
		}

		BlockPos below = pos.below();

		// Something solid to stand on (this is what keeps parrots out of oceans and rivers:
		// water has no collision shape, so their surface is never a valid floor).
		if (level.getBlockState(below).getCollisionShape(level, below).isEmpty()) {
			return Denial.NO_FLOOR;
		}

		// Somewhere to spawn: no block in the way (grass, flowers and air are all fine).
		if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
			return Denial.NO_SPACE;
		}

		// Never inside a fluid.
		if (!level.getFluidState(pos).isEmpty()) {
			return Denial.FLUID;
		}

		// The Nether and the End have no meaningful sky light, so skip the light rule there.
		if (level instanceof Level concreteLevel && !concreteLevel.dimension().equals(Level.OVERWORLD)) {
			return Denial.OK;
		}

		// Same light rule vanilla uses for parrots (> 8).
		return level.getRawBrightness(pos, 0) > 8 ? Denial.OK : Denial.TOO_DARK;
	}

	public static boolean canSpawn(EntityType<Parrot> type, LevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
		return check(type, level, reason, pos, random) == Denial.OK;
	}
}
