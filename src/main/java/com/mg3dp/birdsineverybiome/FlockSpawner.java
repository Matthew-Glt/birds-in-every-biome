package com.mg3dp.birdsineverybiome;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;

/**
 * Vanilla only gives passive mobs a spawn attempt once every 400 ticks (20 seconds), and only while
 * the creature category has room under its cap. That cap is filled by the animals generated together
 * with the world and passive mobs never despawn, so in explored terrain no new animal ever appears -
 * which is why a high spawn weight on its own does nothing. This drops a small flock near each player
 * every so often instead, using the exact same spawn rules as the natural entry.
 */
public final class FlockSpawner {
	public static final int MIN_DISTANCE = 32;
	public static final int MAX_DISTANCE = 56;
	public static final int NEARBY_RADIUS = 64;

	/** Outcome of one flock attempt, with a human readable reason. */
	public record Attempt(boolean spawned, String detail) {}

	private FlockSpawner() {}

	public static void register(BirdsConfig config) {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (!config.flockEnabled) {
				return;
			}

			int interval = Math.max(5, config.flockDelaySeconds) * 20;

			if (server.getTickCount() % interval != 0) {
				return;
			}

			for (ServerLevel level : server.getAllLevels()) {
				for (ServerPlayer player : level.players()) {
					attempt(level, player.blockPosition(), config);
				}
			}
		});
	}

	/** One flock attempt around {@code origin}. */
	public static Attempt attempt(ServerLevel level, BlockPos origin, BirdsConfig config) {
		RandomSource random = level.getRandom();

		int nearby = BirdSpawner.countNearby(level, origin, NEARBY_RADIUS);

		if (nearby >= config.maxBirdsNearby) {
			return new Attempt(false, "already " + nearby + " parrots within " + NEARBY_RADIUS + " blocks (limit " + config.maxBirdsNearby + ")");
		}

		BlockPos spot = BirdSpawner.findSpot(level, origin, MIN_DISTANCE, MAX_DISTANCE, random);

		if (spot == null) {
			return new Attempt(false, "no loaded chunk " + MIN_DISTANCE + "-" + MAX_DISTANCE + " blocks away");
		}

		if (config.skipJungles && level.getBiome(spot).is(BiomeTags.IS_JUNGLE)) {
			return new Attempt(false, "spot " + spot.toShortString() + " is a jungle");
		}

		String biomeName = level.getBiome(spot).getRegisteredName();

		if (!config.isBiomeAllowed(biomeName)) {
			return new Attempt(false, "biome " + biomeName + " is switched off in the config");
		}

		ParrotSpawnRules.Denial denial = ParrotSpawnRules.check(EntityTypes.PARROT, level, EntitySpawnReason.NATURAL, spot, random);

		if (denial != ParrotSpawnRules.Denial.OK) {
			return new Attempt(false, "spot " + spot.toShortString() + " rejected: " + denial.message());
		}

		int spread = Math.max(1, config.maxGroupSize - config.minGroupSize + 1);
		int count = config.minGroupSize + random.nextInt(spread);
		int spawned = BirdSpawner.spawnFlock(level, spot, count, random);

		if (spawned == 0) {
			return new Attempt(false, "spot " + spot.toShortString() + " passed the rules but the game refused the entities");
		}

		return new Attempt(true, "spawned " + spawned + " parrot(s) at " + spot.toShortString()
				+ " (" + level.getBiome(spot).getRegisteredName() + ")");
	}

	public static boolean tryFlockFor(ServerLevel level, BlockPos origin, BirdsConfig config) {
		return attempt(level, origin, config).spawned();
	}
}
