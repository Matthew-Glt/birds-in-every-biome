package com.mg3dp.birdsineverybiome;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.fabricmc.loader.api.FabricLoader;

/**
 * config/birds-in-every-biome.json - edit it and restart the game, no rebuild needed.
 */
public final class BirdsConfig {
	/** How often parrots are picked for a creature spawn in a biome. Vanilla jungles use 5. */
	public int spawnWeight = 4;
	/** Parrots per spawn. */
	public int minGroupSize = 1;
	public int maxGroupSize = 2;
	/** Keep vanilla's own jungle parrots instead of adding a second parrot entry there. */
	public boolean skipJungles = true;
	/** Drop a small flock near each player now and then - see the README for why this is needed. */
	public boolean flockEnabled = true;
	/** Seconds between flock attempts, per player. */
	public int flockDelaySeconds = 30;
	/** Stop adding flocks while this many parrots are already within 64 blocks of a player. */
	public int maxBirdsNearby = 8;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private BirdsConfig() {}

	public static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve("birds-in-every-biome.json");
	}

	/** Called by the Mod Menu config screen. */
	public void save() {
		try {
			Path path = path();
			Files.createDirectories(path.getParent());
			Files.writeString(path, GSON.toJson(this));
			BirdsInEveryBiome.LOGGER.info("Saved config to {}", path);
		} catch (IOException e) {
			BirdsInEveryBiome.LOGGER.warn("Could not save config", e);
		}
	}

	/** Puts every option back to its default value (used by the config screen's button). */
	public void resetToDefaults() {
		BirdsConfig defaults = new BirdsConfig();
		this.spawnWeight = defaults.spawnWeight;
		this.minGroupSize = defaults.minGroupSize;
		this.maxGroupSize = defaults.maxGroupSize;
		this.skipJungles = defaults.skipJungles;
		this.flockEnabled = defaults.flockEnabled;
		this.flockDelaySeconds = defaults.flockDelaySeconds;
		this.maxBirdsNearby = defaults.maxBirdsNearby;
	}

	public static BirdsConfig load() {
		Path path = path();
		BirdsConfig config = new BirdsConfig();

		// The mod used to be called Biome Parrots: carry an old config file over to the new name.
		Path legacy = FabricLoader.getInstance().getConfigDir().resolve("biome-parrots.json");
		Path source = Files.exists(path) ? path : (Files.exists(legacy) ? legacy : null);

		try {
			if (source != null) {
				BirdsConfig read = GSON.fromJson(Files.readString(source), BirdsConfig.class);

				if (read != null) {
					config = read;
				}

				if (source != path) {
					config.save();
					BirdsInEveryBiome.LOGGER.info("Moved old config {} to {}", legacy, path);
				}
			} else {
				Files.createDirectories(path.getParent());
				Files.writeString(path, GSON.toJson(config));
				BirdsInEveryBiome.LOGGER.info("Wrote default config to {}", path);
			}
		} catch (IOException | RuntimeException e) {
			BirdsInEveryBiome.LOGGER.warn("Could not read {} - falling back to defaults", path, e);
		}

		config.spawnWeight = Math.max(0, config.spawnWeight);
		config.minGroupSize = Math.max(1, config.minGroupSize);
		config.maxGroupSize = Math.max(config.minGroupSize, config.maxGroupSize);
		config.flockDelaySeconds = Math.max(5, config.flockDelaySeconds);
		config.maxBirdsNearby = Math.max(1, config.maxBirdsNearby);
		return config;
	}
}
