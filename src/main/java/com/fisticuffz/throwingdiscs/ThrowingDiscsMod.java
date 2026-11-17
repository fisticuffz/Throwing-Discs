package com.fisticuffz.throwingdiscs;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The discs themselves (items, recipes, throw/ricochet/return logic) live in the mod's
 * data/ and assets/ folders and are loaded automatically as a built-in data pack and resource pack.
 */
public class ThrowingDiscsMod implements ModInitializer {
	public static final String MOD_ID = "throwing_discs";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Throwing Discs loaded. Use /function ore_discs:give/all");
	}
}
