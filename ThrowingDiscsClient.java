package com.throwingdiscs.client;

import com.throwingdiscs.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;

public class ThrowingDiscsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Safe default: vanilla thrown-item renderer (a sprite that faces the camera).
		// For the flat, spinning, pitched disc see README.md ("Flat spinning renderer").
		EntityRenderers.register(ModEntities.DISC, ctx -> new ThrownItemRenderer<>(ctx, 1.6f, true));
	}
}
