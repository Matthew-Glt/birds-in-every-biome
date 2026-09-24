package com.mg3dp.birdsineverybiome.client.mixin;

import com.mg3dp.birdsineverybiome.client.AvatarSkinRenderState;

import net.minecraft.client.renderer.entity.state.AvatarRenderState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(AvatarRenderState.class)
public class AvatarRenderStateMixin implements AvatarSkinRenderState {
	@Unique
	private int birdsineverybiome$leftShoulderSkin = 0;

	@Unique
	private int birdsineverybiome$rightShoulderSkin = 0;

	@Override
	public int birdsineverybiome$shoulderSkin(boolean left) {
		return left ? this.birdsineverybiome$leftShoulderSkin : this.birdsineverybiome$rightShoulderSkin;
	}

	@Override
	public void birdsineverybiome$setShoulderSkin(boolean left, int skin) {
		if (left) {
			this.birdsineverybiome$leftShoulderSkin = skin;
		} else {
			this.birdsineverybiome$rightShoulderSkin = skin;
		}
	}
}
