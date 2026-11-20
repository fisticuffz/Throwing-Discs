package com.throwingdiscs.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.throwingdiscs.ThrowingDiscEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.entity.state.ThrownItemRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Draws the disc item lying flat (its plane contains the direction of travel), pitched along the path,
 * leaning by the synced tilt and spinning around its own axis. Item model + lighting come from the vanilla
 * ThrownItemRenderer; only the pose is ours.
 */
public class ThrowingDiscRenderer extends ThrownItemRenderer<ThrowingDiscEntity> {
	private final ItemModelResolver discItemResolver;

	public ThrowingDiscRenderer(EntityRendererProvider.Context context) {
		super(context, 1.5f, true);
		this.discItemResolver = context.getItemModelResolver();
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
			// Re-resolve the item with NO display transform (the default "on the ground" transform shifts the
			// sprite off-centre, which made the spin wobble around a pivot on the side).
			this.discItemResolver.updateForNonLiving(d.item, entity.getItem(), ItemDisplayContext.NONE, entity);
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
		// Direction of travel from the (smoothed) yaw/pitch, in world space.
		float yawR = d.yaw * Mth.DEG_TO_RAD;
		float pitchR = d.pitch * Mth.DEG_TO_RAD;
		Vector3f f = new Vector3f(-Mth.sin(yawR) * Mth.cos(pitchR), -Mth.sin(pitchR), Mth.cos(yawR) * Mth.cos(pitchR));

		// Normal of the flat disc: "up", made perpendicular to the direction of travel,
		// so the disc's plane ALWAYS contains the direction it is flying in.
		Vector3f n = new Vector3f(0f, 1f, 0f).sub(new Vector3f(f).mul(f.y));
		if (n.lengthSquared() < 1e-6f) n.set(-Mth.sin(yawR), 0f, Mth.cos(yawR));   // flying straight up/down
		n.normalize();
		n.rotateAxis(d.tilt * Mth.DEG_TO_RAD, f.x, f.y, f.z);                      // slight lean around the flight direction

		// The item sprite faces +Z, so turn +Z onto the disc normal, then spin around that normal.
		Quaternionf orientation = new Quaternionf().rotationTo(0f, 0f, 1f, n.x, n.y, n.z);

		poseStack.pushPose();
		poseStack.rotate(orientation);
		poseStack.rotate(Axis.ZP.rotationDegrees(d.spin));     // spin around its own axis
		poseStack.scale(0.75f, 0.75f, 0.75f);                  // sprite is 1 block wide with no display transform; ring is ~0.56 block
		d.item.submit(poseStack, collector, d.lightCoords, OverlayTexture.NO_OVERLAY, d.outlineColor);
		poseStack.popPose();
	}
}
