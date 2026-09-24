package com.mg3dp.birdsineverybiome.client;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.function.IntConsumer;

import com.mg3dp.birdsineverybiome.BirdsConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.biome.Biome;

/**
 * Opened from Mod Menu. Plain vanilla widgets on purpose, so the only optional dependency is
 * Mod Menu itself. Two pages: the spawn settings, and a toggle for every biome.
 *
 * <p>Both pages edit the same in-memory config. Nothing reaches disk until Save is pressed, and the
 * game reads the file once at startup, so changes apply on the next launch.
 */
public class BirdsConfigScreen extends Screen {
	private static final int ROW_WIDTH = 310;
	private static final int ROW_HEIGHT = 20;
	private static final int GAP = 2;
	private static final int BIOMES_PER_PAGE = 7;

	private final Screen parent;
	private final BirdsConfig config;
	private final List<String> biomeIds;

	private boolean biomesPage = false;
	private int page = 0;

	public BirdsConfigScreen(Screen parent) {
		super(Component.literal("Birds in Every Biome"));
		this.parent = parent;
		this.config = BirdsConfig.load();
		this.biomeIds = collectBiomeIds(this.config);
	}

	/**
	 * The vanilla biome list, plus the live registry when a world is loaded (datapack and modded
	 * biomes), plus anything already switched off so it can always be switched back on.
	 */
	private static List<String> collectBiomeIds(BirdsConfig config) {
		TreeSet<String> ids = new TreeSet<>(VanillaBiomes.IDS);
		Minecraft minecraft = Minecraft.getInstance();

		if (minecraft.level != null) {
			Registry<Biome> registry = minecraft.level.registryAccess().lookupOrThrow(Registries.BIOME);
			registry.keySet().forEach(id -> ids.add(id.toString()));
		}

		ids.addAll(config.disabledBiomes);
		return new ArrayList<>(ids);
	}

	@Override
	protected void init() {
		super.init();

		if (this.biomesPage) {
			this.initBiomesPage();
		} else {
			this.initSettingsPage();
		}
	}

	// ---------------------------------------------------------------- page 1: spawn settings

	private void initSettingsPage() {
		int x = this.width / 2 - ROW_WIDTH / 2;
		int y = 26;

		this.addRenderableWidget(new IntSlider(x, y, this.config.spawnWeight, 0, 50, "Spawn weight",
				value -> this.config.spawnWeight = value,
				"How often parrots are picked when a creature spawn happens, relative to the biome's other animals. Vanilla jungles use 5. 0 disables biome parrots."));
		y += ROW_HEIGHT + GAP;

		this.addRenderableWidget(new IntSlider(x, y, this.config.minGroupSize, 1, 8, "Min group size",
				value -> this.config.minGroupSize = value,
				"Fewest parrots in one spawn."));
		y += ROW_HEIGHT + GAP;

		this.addRenderableWidget(new IntSlider(x, y, this.config.maxGroupSize, 1, 8, "Max group size",
				value -> this.config.maxGroupSize = value,
				"Most parrots in one spawn."));
		y += ROW_HEIGHT + GAP;

		CycleButton<Boolean> skipJungles = CycleButton.onOffBuilder(this.config.skipJungles)
				.create(x, y, ROW_WIDTH, ROW_HEIGHT, Component.literal("Skip jungles"),
						(button, value) -> this.config.skipJungles = value);
		skipJungles.setTooltip(Tooltip.create(Component.literal(
				"Leave vanilla's own jungle parrots alone instead of adding a second parrot entry there.")));
		this.addRenderableWidget(skipJungles);
		y += ROW_HEIGHT + GAP;

		CycleButton<Boolean> flocks = CycleButton.onOffBuilder(this.config.flockEnabled)
				.create(x, y, ROW_WIDTH, ROW_HEIGHT, Component.literal("Flock spawning"),
						(button, value) -> this.config.flockEnabled = value);
		flocks.setTooltip(Tooltip.create(Component.literal(
				"Vanilla's passive spawner is capped and throttled, so it almost never fires in an explored world. This drops a small flock near players instead.")));
		this.addRenderableWidget(flocks);
		y += ROW_HEIGHT + GAP;

		this.addRenderableWidget(new IntSlider(x, y, this.config.flockDelaySeconds, 5, 300, "Flock delay (seconds)",
				value -> this.config.flockDelaySeconds = value,
				"Seconds between flock attempts, per player. Lower = more birds."));
		y += ROW_HEIGHT + GAP;

		this.addRenderableWidget(new IntSlider(x, y, this.config.maxBirdsNearby, 1, 32, "Max birds nearby",
				value -> this.config.maxBirdsNearby = value,
				"Stop adding flocks while this many parrots are already within 64 blocks of a player."));
		y += ROW_HEIGHT + GAP * 2;

		int half = (ROW_WIDTH - GAP) / 2;

		this.addRenderableWidget(Button.builder(Component.literal("Restore defaults"), button -> {
			this.config.resetToDefaults();
			this.rebuildWidgets();
		}).bounds(x, y, half, ROW_HEIGHT)
				.tooltip(Tooltip.create(Component.literal(
						"Put every option back to its default value, including the biome toggles. Press Save to write it to disk.")))
				.build());

		this.addRenderableWidget(Button.builder(
				Component.literal("Biome toggles (" + this.config.disabledBiomes.size() + " off)"), button -> {
					this.biomesPage = true;
					this.page = 0;
					this.rebuildWidgets();
				}).bounds(x + half + GAP, y, half, ROW_HEIGHT)
				.tooltip(Tooltip.create(Component.literal(
						"Choose which biomes parrots may spawn in.")))
				.build());
		y += ROW_HEIGHT + GAP * 2;

		this.addRenderableWidget(Button.builder(Component.literal("Save"), button -> {
			this.config.save();
			Minecraft.getInstance().setScreenAndShow(this.parent);
		}).bounds(x, y, half, ROW_HEIGHT)
				.tooltip(Tooltip.create(Component.literal(
						"Writes config/birds-in-every-biome.json. Spawn rates and biome toggles apply the next time the world loads.")))
				.build());

		this.addRenderableWidget(Button.builder(Component.literal("Cancel"),
						button -> Minecraft.getInstance().setScreenAndShow(this.parent))
				.bounds(x + half + GAP, y, half, ROW_HEIGHT).build());
	}

	// ---------------------------------------------------------------- page 2: biome toggles

	private void initBiomesPage() {
		int x = this.width / 2 - ROW_WIDTH / 2;
		int y = 22;
		int half = (ROW_WIDTH - GAP) / 2;

		int pages = this.pageCount();
		this.page = Math.max(0, Math.min(this.page, pages - 1));
		int from = this.page * BIOMES_PER_PAGE;

		for (int i = 0; i < BIOMES_PER_PAGE; i++) {
			int index = from + i;

			if (index >= this.biomeIds.size()) {
				break;
			}

			String biomeId = this.biomeIds.get(index);

			CycleButton<Boolean> toggle = CycleButton.onOffBuilder(this.config.isBiomeAllowed(biomeId))
					.create(x, y, ROW_WIDTH, ROW_HEIGHT, Component.literal(shortName(biomeId)),
							(button, value) -> this.config.setBiomeAllowed(biomeId, value));
			toggle.setTooltip(Tooltip.create(Component.literal(biomeId + (this.config.isBiomeAllowed(biomeId)
					? " - parrots may spawn here"
					: " - parrots will not spawn here"))));
			this.addRenderableWidget(toggle);
			y += ROW_HEIGHT + GAP;
		}

		Button previous = Button.builder(Component.literal("< Previous"), button -> {
			this.page--;
			this.rebuildWidgets();
		}).bounds(x, y, 80, ROW_HEIGHT).build();
		previous.active = this.page > 0;
		this.addRenderableWidget(previous);

		Button indicator = Button.builder(Component.literal(
				"Biomes  " + (this.page + 1) + " / " + pages + "   (" + this.biomeIds.size() + " total)"),
				button -> {
				}).bounds(x + 80 + GAP, y, ROW_WIDTH - 2 * (80 + GAP), ROW_HEIGHT).build();
		indicator.active = false;
		this.addRenderableWidget(indicator);

		Button next = Button.builder(Component.literal("Next >"), button -> {
			this.page++;
			this.rebuildWidgets();
		}).bounds(x + ROW_WIDTH - 80, y, 80, ROW_HEIGHT).build();
		next.active = this.page < pages - 1;
		this.addRenderableWidget(next);
		y += ROW_HEIGHT + GAP * 2;

		this.addRenderableWidget(Button.builder(Component.literal("Enable all"), button -> {
			for (String biomeId : this.biomeIds) {
				this.config.setBiomeAllowed(biomeId, true);
			}

			this.rebuildWidgets();
		}).bounds(x, y, half, ROW_HEIGHT)
				.tooltip(Tooltip.create(Component.literal("Let parrots spawn in every biome.")))
				.build());

		this.addRenderableWidget(Button.builder(Component.literal("Disable all"), button -> {
			for (String biomeId : this.biomeIds) {
				this.config.setBiomeAllowed(biomeId, false);
			}

			this.rebuildWidgets();
		}).bounds(x + half + GAP, y, half, ROW_HEIGHT)
				.tooltip(Tooltip.create(Component.literal(
						"Switch every listed biome off. Vanilla's own jungle parrots are unaffected unless Skip jungles is on.")))
				.build());
		y += ROW_HEIGHT + GAP * 2;

		this.addRenderableWidget(Button.builder(Component.literal("Back"), button -> {
			this.biomesPage = false;
			this.rebuildWidgets();
		}).bounds(x, y, half, ROW_HEIGHT)
				.tooltip(Tooltip.create(Component.literal("Back to the spawn settings.")))
				.build());

		this.addRenderableWidget(Button.builder(Component.literal("Save"), button -> {
			this.config.save();
			Minecraft.getInstance().setScreenAndShow(this.parent);
		}).bounds(x + half + GAP, y, half, ROW_HEIGHT)
				.tooltip(Tooltip.create(Component.literal(
						"Writes config/birds-in-every-biome.json. Biome toggles apply the next time the world loads.")))
				.build());
	}

	private int pageCount() {
		return Math.max(1, (this.biomeIds.size() + BIOMES_PER_PAGE - 1) / BIOMES_PER_PAGE);
	}

	private static String shortName(String biomeId) {
		return biomeId.startsWith("minecraft:") ? biomeId.substring("minecraft:".length()) : biomeId;
	}

	// ---------------------------------------------------------------- shared widgets

	private static class IntSlider extends AbstractSliderButton {
		private final int min;
		private final int max;
		private final String label;
		private final IntConsumer onChange;

		IntSlider(int x, int y, int initial, int min, int max, String label, IntConsumer onChange, String tooltip) {
			super(x, y, ROW_WIDTH, ROW_HEIGHT, Component.empty(), toFraction(initial, min, max));
			this.min = min;
			this.max = max;
			this.label = label;
			this.onChange = onChange;
			this.setTooltip(Tooltip.create(Component.literal(tooltip)));
			this.updateMessage();
		}

		private static double toFraction(int value, int min, int max) {
			return max <= min ? 0.0D : (double) (value - min) / (double) (max - min);
		}

		@Override
		protected void updateMessage() {
			// Also called from the super constructor, before our fields exist.
			if (this.label == null) {
				return;
			}

			this.setMessage(Component.literal(this.label + ": " + this.currentValue()));
		}

		@Override
		protected void applyValue() {
			this.onChange.accept(this.currentValue());
		}

		private int currentValue() {
			// 'value' is AbstractSliderButton's protected 0..1 fraction, set before the callbacks run.
			return (int) Math.round(this.min + this.value * (this.max - this.min));
		}
	}
}
