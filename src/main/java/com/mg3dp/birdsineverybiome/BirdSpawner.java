package com.mg3dp.birdsineverybiome;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

/** Creates parrots the way vanilla's spawner would: surface position, same spawn rules, real entity. */
public final class BirdSpawner {
	private BirdSpawner() {}

	/**
	 * A surface position {@code minDistance}..{@code maxDistance} blocks away from the origin, or null
	 * if no loaded chunk was found there.
	 *
	 * <p>Unloaded or not-yet-generated chunks report the minimum build height for every column, so any
	 * candidate sitting on the very bottom of the world is thrown away instead of being used - otherwise
	 * a flock would aim at y=-64 and silently fail.
	 */
	public static BlockPos findSpot(ServerLevel level, BlockPos origin, int minDistance, int maxDistance, RandomSource random) {
		int minY = level.getMinY();

		for (int attempt = 0; attempt < 8; attempt++) {
			double angle = random.nextDouble() * Math.PI * 2.0D;
			int distance = minDistance + random.nextInt(Math.max(1, maxDistance - minDistance));
			int x = origin.getX() + (int) Math.round(Math.cos(angle) * distance);
			int z = origin.getZ() + (int) Math.round(Math.sin(angle) * distance);
			int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);

			if (y <= minY + 2) {
				continue;
			}

			return new BlockPos(x, y, z);
		}

		return null;
	}

	/** @return how many parrots were actually added to the level. */
	public static int spawnFlock(ServerLevel level, BlockPos pos, int count, RandomSource random) {
		int spawned = 0;

		for (int i = 0; i < count; i++) {
			Parrot parrot = EntityTypes.PARROT.create(level, EntitySpawnReason.NATURAL);

			if (parrot == null) {
				continue;
			}

			parrot.snapTo(
					pos.getX() + 0.5D + (random.nextDouble() - 0.5D) * 2.0D,
					pos.getY(),
					pos.getZ() + 0.5D + (random.nextDouble() - 0.5D) * 2.0D,
					random.nextFloat() * 360.0F,
					0.0F);
			// This is what gives the parrot its biome skin.
			parrot.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.NATURAL, null);

			if (level.addFreshEntity(parrot)) {
				spawned++;
			}
		}

		return spawned;
	}

	public static int countNearby(ServerLevel level, BlockPos pos, int radius) {
		AABB box = new AABB(
				pos.getX() - radius, pos.getY() - radius, pos.getZ() - radius,
				pos.getX() + radius + 1, pos.getY() + radius + 1, pos.getZ() + radius + 1);
		return level.getEntitiesOfClass(Parrot.class, box).size();
	}
}
