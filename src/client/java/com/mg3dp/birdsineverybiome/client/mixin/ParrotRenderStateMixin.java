package com.mg3dp.birdsineverybiome.client.mixin;

import com.mg3dp.birdsineverybiome.client.ParrotSkinRenderState;

import net.minecraft.client.renderer.entity.state.ParrotRenderState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ParrotRenderState.class)
public class ParrotRenderStateMixin implements ParrotSkinRenderState {
	@Unique
	private int birdsineverybiome$skin = 0;

	@Override
	public int birdsineverybiome$skin() {
		return this.birdsineverybiome$skin;
	}

	@Override
	public void birdsineverybiome$setSkin(int index) {
		this.birdsineverybiome$skin = index;
	}
}
