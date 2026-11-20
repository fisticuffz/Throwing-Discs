package com.throwingdiscs.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.throwingdiscs.ThrowingDiscEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Matrix4f;

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
		// blend between the last and current tick so the turning is smooth at any frame rate / tick rate
		// At normal speed the game's own position is already smooth, so use it as is. Only when the world runs slower
		// than 20 ticks per second (/tick rate) blend our own previous-tick -> current-tick values, so the disc doesn't hop.
		boolean slow = entity.level().tickRateManager().tickrate() < 19.5f;
		if (slow) {
			state.x = Mth.lerp((double) partialTick, entity.smX0, entity.smX1);
			state.y = Mth.lerp((double) partialTick, entity.smY0, entity.smY1);
			state.z = Mth.lerp((double) partialTick, entity.smZ0, entity.smZ1);
			state.yRot = Mth.rotLerp(partialTick, entity.smYaw0, entity.smYaw1);
			state.xRot = com.throwingdiscs.DiscConfig.KEEP_LEVEL ? 0f : Mth.lerp(partialTick, entity.smPitch0, entity.smPitch1);
		} else {
			state.yRot = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
			state.xRot = com.throwingdiscs.DiscConfig.KEEP_LEVEL ? 0f : Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
		}
		state.tilt = entity.getTilt();
		state.spin = Mth.lerp(partialTick, entity.spinO, entity.spin);
		// FIXED, not GROUND: the GROUND display transform shifts the model up by 2/16 of a block, and because the spin is around
		// the sprite's own axis that shift made the disc orbit a point off to one side instead of spinning on its centre.
		this.itemModelResolver.updateForNonLiving(state.item, entity.getItem(), ItemDisplayContext.FIXED, entity);
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		poseStack.pushPose();
		// 26.3: PoseStack.mulPose takes a matrix, so the rotations are chained on one Matrix4f (applied in this order).
		Matrix4f m = new Matrix4f()
				.rotateY((float) Math.toRadians(180f - state.yRot))   // face travel direction
				.rotateX((float) Math.toRadians(state.xRot))          // pitch along the path
				.rotateZ((float) Math.toRadians(state.tilt))          // lean (return trip only)
				.rotateX((float) Math.toRadians(90f))                 // lay the sprite flat
				.rotateZ((float) Math.toRadians(state.spin));         // spin around its own axis
		poseStack.mulPose(m);
		poseStack.scale(1.6f, 1.6f, 1.6f);
		state.item.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
		poseStack.popPose();
	}
}
