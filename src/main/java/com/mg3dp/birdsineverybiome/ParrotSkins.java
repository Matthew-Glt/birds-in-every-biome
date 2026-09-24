package com.mg3dp.birdsineverybiome;

import java.util.List;
import java.util.Set;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

/**
 * The biome -> texture table. Two things to edit if you add your own skins:
 *   1. add the skin's name to {@link #SKIN_NAMES} (the name is also the texture file name)
 *   2. add a {@link Rule} for it to {@link #RULES}
 */
public final class ParrotSkins {
	/** Index 0 must stay "vanilla": it means "use the five normal parrot colours". */
	public static final List<String> SKIN_NAMES = List.of(
			"vanilla",
			"arid",
			"snowy",
			"mushroom",
			"nether",
			"end"
	);

	/** index into SKIN_NAMES -> the texture this skin uses. */
	public static final int VANILLA = 0;

	private record Rule(String skin, Set<String> biomeIds, Set<TagKey<Biome>> biomeTags) {}

	/** First matching rule wins, so put the specific ones first. */
	private static final List<Rule> RULES = List.of(
			new Rule("arid", Set.of("minecraft:desert"),
					Set.of(BiomeTags.IS_BADLANDS, BiomeTags.IS_SAVANNA)),
			new Rule("snowy", Set.of(
					"minecraft:snowy_plains", "minecraft:ice_spikes", "minecraft:snowy_taiga",
					"minecraft:snowy_beach", "minecraft:snowy_slopes", "minecraft:frozen_peaks",
					"minecraft:jagged_peaks", "minecraft:grove", "minecraft:frozen_river",
					"minecraft:frozen_ocean", "minecraft:deep_frozen_ocean"), Set.of()),
			new Rule("mushroom", Set.of("minecraft:mushroom_fields"), Set.of()),
			new Rule("nether", Set.of(), Set.of(BiomeTags.IS_NETHER)),
			new Rule("end", Set.of(), Set.of(BiomeTags.IS_END))
	);

	private ParrotSkins() {}

	/** Which skin a freshly spawned parrot gets, based on the biome it is standing in. */
	public static String skinFor(Holder<Biome> biome) {
		for (Rule rule : RULES) {
			for (TagKey<Biome> tag : rule.biomeTags()) {
				if (biome.is(tag)) {
					return rule.skin();
				}
			}
			for (String id : rule.biomeIds()) {
				if (biome.is(Identifier.parse(id))) {
					return rule.skin();
				}
			}
		}
		return SKIN_NAMES.get(VANILLA);
	}

	public static int indexOf(String skinName) {
		int index = SKIN_NAMES.indexOf(skinName);
		return index < 0 ? VANILLA : index;
	}

	public static String skinName(int index) {
		return index >= 0 && index < SKIN_NAMES.size() ? SKIN_NAMES.get(index) : SKIN_NAMES.get(VANILLA);
	}

	/** Texture used on the client for a skin index. */
	public static Identifier texture(int index) {
		return BirdsInEveryBiome.id("textures/entity/parrot/parrot_" + skinName(index) + ".png");
	}
}
