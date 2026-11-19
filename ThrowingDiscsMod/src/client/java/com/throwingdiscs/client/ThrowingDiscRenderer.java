package com.throwingdiscs.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.throwingdiscs.ThrowingDiscEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.entity.state.ThrownItemRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;

/**
 * Draws the disc item lying flat (its plane contains the direction of travel), pitched along the path,
 * leaning by the synced tilt and spinning around its own axis. Item model + lighting come from the vanilla
 * ThrownItemRenderer; only the pose is ours.
 */
public class ThrowingDiscRenderer extends ThrownItemRenderer<ThrowingDiscEntity> {
	public ThrowingDiscRenderer(EntityRendererProvider.Context context) {
		super(context, 1.5f, true);
	}

	public static class DiscRenderState extends ThrownItemRenderState {
		public float yaw, pitch, tilt, spin;
	}

	@Override
	public ThrownItemRenderState createRenderState() {
		return new DiscRenderState();
	}

	@Override
	public void extractRenderState(ThrowingDiscEntity entity, ThrownItemRenderState state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		if (state instanceof DiscRenderState d) {
			d.yaw = Mth.rotLerp(partialTick, entity.viewYawO, entity.viewYaw);
			d.pitch = Mth.lerp(partialTick, entity.viewPitchO, entity.viewPitch);
			d.tilt = entity.getTilt();
			d.spin = Mth.lerp(partialTick, entity.spinO, entity.spin);
		}
	}

	@Override
	public void submit(ThrownItemRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		if (!(state instanceof DiscRenderState d)) {
			super.submit(state, poseStack, collector, camera);
			return;
		}
		poseStack.pushPose();
		poseStack.rotate(Axis.YP.rotationDegrees(-d.yaw));     // face the travel direction
		poseStack.rotate(Axis.XP.rotationDegrees(d.pitch));    // pitch along the path
		poseStack.rotate(Axis.ZP.rotationDegrees(d.tilt));     // lean (return trip only)
		poseStack.rotate(Axis.XP.rotationDegrees(90f));        // lay the sprite flat
		poseStack.rotate(Axis.ZP.rotationDegrees(d.spin));     // spin around its own axis
		poseStack.scale(1.5f, 1.5f, 1.5f);                     // about 0.75 block wide
		d.item.submit(poseStack, collector, d.lightCoords, OverlayTexture.NO_OVERLAY, d.outlineColor);
		poseStack.popPose();
	}
}
