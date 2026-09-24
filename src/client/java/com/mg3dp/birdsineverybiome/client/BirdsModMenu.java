package com.mg3dp.birdsineverybiome.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/** Gives Mod Menu a "Config" button for this mod. Only loaded when Mod Menu is installed. */
@Environment(EnvType.CLIENT)
public class BirdsModMenu implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return BirdsConfigScreen::new;
	}
}
