package com.mg3dp.birdsineverybiome.client;

import java.util.function.IntConsumer;

import com.mg3dp.birdsineverybiome.BirdsConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Opened from Mod Menu. Plain vanilla widgets on purpose, so the only optional dependency is
 * Mod Menu itself.
 */
public class BirdsConfigScreen extends Screen {
	private static final int ROW_WIDTH = 310;
	private static final int ROW_HEIGHT = 20;
	private static final int GAP = 2;

	private final Screen parent;
	private final BirdsConfig config;

	public BirdsConfigScreen(Screen parent) {
		super(Component.literal("Birds in Every Biome"));
		this.parent = parent;
		this.config = BirdsConfig.load();
	}

	@Override
	protected void init() {
		super.init();

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
		y += ROW_HEIGHT + GAP * 3;

		this.addRenderableWidget(Button.builder(Component.literal("Restore defaults"), button -> {
			this.config.resetToDefaults();
			this.rebuildWidgets();
		}).bounds(x, y, ROW_WIDTH, ROW_HEIGHT)
				.tooltip(Tooltip.create(Component.literal(
						"Put every option back to its default value. Press Save to write it to disk.")))
				.build());
		y += ROW_HEIGHT + GAP * 2;

		int half = (ROW_WIDTH - GAP) / 2;
		this.addRenderableWidget(Button.builder(Component.literal("Save"), button -> {
			this.config.save();
			Minecraft.getInstance().setScreenAndShow(this.parent);
		}).bounds(x, y, half, ROW_HEIGHT)
				.tooltip(Tooltip.create(Component.literal(
						"Writes config/birds-in-every-biome.json. Spawn rates apply the next time the world loads.")))
				.build());

		this.addRenderableWidget(Button.builder(Component.literal("Cancel"),
						button -> Minecraft.getInstance().setScreenAndShow(this.parent))
				.bounds(x + half + GAP, y, half, ROW_HEIGHT).build());
	}

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
