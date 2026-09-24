package com.mg3dp.birdsineverybiome;

/**
 * Implemented on {@code Player} by {@link com.mg3dp.birdsineverybiome.mixin.PlayerMixin}, so the biome
 * skin of the parrot sitting on each shoulder is synced to every client.
 */
public interface PlayerShoulderSkins {
	/** 0 = vanilla (normal parrot variant texture). */
	int birdsineverybiome$shoulderSkin(boolean left);

	void birdsineverybiome$setShoulderSkin(boolean left, int skin);

	/** Server side: remember the skin of the parrot that is about to land on a shoulder. */
	void birdsineverybiome$setPendingShoulderSkin(int skin);
}
