package com.throwingdiscs.client;

import com.throwingdiscs.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.entity.EntityRenderers;

public class ThrowingDiscsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Flat, spinning, pitched 3D disc. To fall back to vanilla's sprite renderer, use:
		// EntityRenderers.register(ModEntities.DISC, ctx -> new net.minecraft.client.renderer.entity.ThrownItemRenderer<>(ctx, 1.6f, true));
		EntityRenderers.register(ModEntities.DISC, ThrowingDiscFlatRenderer::new);
	}
}
