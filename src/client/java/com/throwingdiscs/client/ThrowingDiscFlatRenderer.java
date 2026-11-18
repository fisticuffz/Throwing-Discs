package com.throwingdiscs.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.throwingdiscs.ThrowingDiscEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;   // <- if this import is red, let the IDE find the right package
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;

/**
 * Draws the disc item flat, spinning (900 deg/s), pitched along its path and leaning by the synced tilt.
 * Written against the 1.21.9+ "submit" renderer API; if something is red, use your IDE's quick fix / the
 * decompiled ThrownItemRenderer as a template (the structure is the same).
 */
public class ThrowingDiscFlatRenderer extends EntityRenderer<ThrowingDiscEntity, ThrowingDiscFlatRenderer.State> {
	private final ItemModelResolver itemModelResolver;

	public ThrowingDiscFlatRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.itemModelResolver = context.getItemModelResolver();
	}

	public static class State extends EntityRenderState {
		public final ItemStackRenderState item = new ItemStackRenderState();
		public float yRot, xRot, tilt, spin;
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(ThrowingDiscEntity entity, State state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		state.yRot = entity.getYRot();
		state.xRot = entity.getXRot();
		state.tilt = entity.getTilt();
		state.spin = Mth.lerp(partialTick, entity.spinO, entity.spin);
		this.itemModelResolver.updateForNonLiving(state.item, entity.getItem(), ItemDisplayContext.GROUND, entity);
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		poseStack.pushPose();
		poseStack.mulPose(Axis.YP.rotationDegrees(180f - state.yRot));   // face travel direction
		poseStack.mulPose(Axis.XP.rotationDegrees(state.xRot));          // pitch along the path
		poseStack.mulPose(Axis.ZP.rotationDegrees(state.tilt));          // lean (return trip only)
		poseStack.mulPose(Axis.XP.rotationDegrees(90f));                 // lay the sprite flat
		poseStack.mulPose(Axis.ZP.rotationDegrees(state.spin));          // spin around its own axis
		poseStack.scale(1.6f, 1.6f, 1.6f);
		state.item.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
		poseStack.popPose();
	}
}
