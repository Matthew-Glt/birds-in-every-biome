package com.mg3dp.birdsineverybiome;

import java.util.function.Predicate;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MobCategory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BirdsInEveryBiome implements ModInitializer {
	public static final String MOD_ID = "birdsineverybiome";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// Spawn rate lives in config/birds-in-every-biome.json - see README.
		BirdsConfig config = BirdsConfig.load();

		// Add a parrot spawn entry to every biome that does not already have one.
		// Jungles are skipped by default because vanilla already spawns parrots there, and biomes
		// switched off in the config screen get no entry at all.
		Predicate<BiomeSelectionContext> selector = context -> {
			if (config.skipJungles && context.hasTag(BiomeTags.IS_JUNGLE)) {
				return false;
			}

			return config.isBiomeAllowed(context.getBiomeKey());
		};

		BiomeModifications.addSpawn(
				selector,
				MobCategory.CREATURE,
				EntityTypes.PARROT,
				config.spawnWeight, config.minGroupSize, config.maxGroupSize);

		FlockSpawner.register(config);
		BirdsCommand.register(config);

		LOGGER.info("Parrots will now spawn in every biome (weight {}, groups of {}-{}, jungles skipped: {}, flocks: {})",
				config.spawnWeight, config.minGroupSize, config.maxGroupSize, config.skipJungles,
				config.flockEnabled ? "every " + config.flockDelaySeconds + "s" : "off");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
