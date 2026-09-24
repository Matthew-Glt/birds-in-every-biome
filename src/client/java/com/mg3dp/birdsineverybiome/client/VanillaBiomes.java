package com.mg3dp.birdsineverybiome.client;

import java.util.List;

/**
 * The vanilla biome ids for the Minecraft version this mod targets.
 *
 * <p>Biomes are a dynamic registry, so the client has no biome registry until a world is loaded -
 * this list is what the config screen shows at the main menu. When a world is loaded the screen
 * merges the live registry in, which adds datapack and modded biomes on top.
 */
public final class VanillaBiomes {
	public static final List<String> IDS = List.of(
			"minecraft:badlands",
			"minecraft:bamboo_jungle",
			"minecraft:basalt_deltas",
			"minecraft:beach",
			"minecraft:birch_forest",
			"minecraft:cherry_grove",
			"minecraft:cold_ocean",
			"minecraft:crimson_forest",
			"minecraft:dappled_forest",
			"minecraft:dark_forest",
			"minecraft:deep_cold_ocean",
			"minecraft:deep_dark",
			"minecraft:deep_frozen_ocean",
			"minecraft:deep_lukewarm_ocean",
			"minecraft:deep_ocean",
			"minecraft:desert",
			"minecraft:dripstone_caves",
			"minecraft:end_barrens",
			"minecraft:end_highlands",
			"minecraft:end_midlands",
			"minecraft:eroded_badlands",
			"minecraft:flower_forest",
			"minecraft:forest",
			"minecraft:frozen_ocean",
			"minecraft:frozen_peaks",
			"minecraft:frozen_river",
			"minecraft:grove",
			"minecraft:ice_spikes",
			"minecraft:jagged_peaks",
			"minecraft:jungle",
			"minecraft:lukewarm_ocean",
			"minecraft:lush_caves",
			"minecraft:mangrove_swamp",
			"minecraft:meadow",
			"minecraft:mushroom_fields",
			"minecraft:nether_wastes",
			"minecraft:ocean",
			"minecraft:old_growth_birch_forest",
			"minecraft:old_growth_pine_taiga",
			"minecraft:old_growth_spruce_taiga",
			"minecraft:pale_garden",
			"minecraft:plains",
			"minecraft:river",
			"minecraft:savanna",
			"minecraft:savanna_plateau",
			"minecraft:small_end_islands",
			"minecraft:snowy_beach",
			"minecraft:snowy_plains",
			"minecraft:snowy_slopes",
			"minecraft:snowy_taiga",
			"minecraft:soul_sand_valley",
			"minecraft:sparse_jungle",
			"minecraft:stony_peaks",
			"minecraft:stony_shore",
			"minecraft:sulfur_caves",
			"minecraft:sunflower_plains",
			"minecraft:swamp",
			"minecraft:taiga",
			"minecraft:the_end",
			"minecraft:the_void",
			"minecraft:warm_ocean",
			"minecraft:warped_forest",
			"minecraft:windswept_forest",
			"minecraft:windswept_gravelly_hills",
			"minecraft:windswept_hills",
			"minecraft:windswept_savanna",
			"minecraft:wooded_badlands"
	);

	private VanillaBiomes() {}
}
