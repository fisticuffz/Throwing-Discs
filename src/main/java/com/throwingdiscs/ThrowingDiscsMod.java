package com.throwingdiscs;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThrowingDiscsMod implements ModInitializer {
	public static final String MOD_ID = "throwing_discs";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		DiscConfig.load();
		ModEntities.initialize();
		ModItems.initialize();
		// leaving the world: discs still flying go back into the player's inventory before they are saved
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> ThrowingDiscEntity.recallAll(handler.getPlayer()));
	}
}
