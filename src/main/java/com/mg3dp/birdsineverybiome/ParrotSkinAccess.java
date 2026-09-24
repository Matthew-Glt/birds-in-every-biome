package com.mg3dp.birdsineverybiome;

/**
 * Implemented on {@code Parrot} by {@link com.mg3dp.birdsineverybiome.mixin.ParrotMixin}.
 * The index is synced to clients through the entity data tracker and saved to NBT by name,
 * so reordering {@link ParrotSkins#SKIN_NAMES} never breaks existing parrots.
 */
public interface ParrotSkinAccess {
	/** 0 = vanilla (use the normal random parrot variant), anything else = index into {@link ParrotSkins#SKIN_NAMES}. */
	int birdsineverybiome$skin();

	void birdsineverybiome$setSkin(int index);
}
