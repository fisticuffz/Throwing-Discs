package com.throwingdiscs.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.throwingdiscs.DiscConfig;
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

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Draws the disc item facing its direction of travel, spinning (900 deg/s) and leaning by the synced tilt.
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

	/** Remembers when each server position arrived, so the disc can be drawn slightly in the past, between two real positions. */
	private static final class Smoother {
		static final int N = 8;
		final long[] t = new long[N];
		final double[] x = new double[N], y = new double[N], z = new double[N];
		final float[] yaw = new float[N], pitch = new float[N];
		int count, head;   // head = index of the newest entry

		int idx(int i) {   // i = 0 oldest ... count-1 newest
			return ((head - (count - 1) + i) % N + N) % N;
		}

		void record(long now, ThrowingDiscEntity e) {
			if (count > 0) {
				int n = idx(count - 1);
				if (x[n] == e.getX() && y[n] == e.getY() && z[n] == e.getZ()) return;   // nothing new arrived
				double dx = e.getX() - x[n], dy = e.getY() - y[n], dz = e.getZ() - z[n];
				if (dx * dx + dy * dy + dz * dz > 64.0) count = 0;   // teleport: don't slide across the map
			}
			head = (head + 1) % N;
			if (count < N) count++;
			t[head] = now; x[head] = e.getX(); y[head] = e.getY(); z[head] = e.getZ();
			yaw[head] = e.getYRot(); pitch[head] = e.getXRot();
		}

		void sample(long rt, State st) {
			int o = idx(0), n = idx(count - 1);
			if (count == 1 || rt <= t[o]) { set(st, o, o, 0f); return; }
			if (rt >= t[n]) { set(st, n, n, 0f); return; }
			for (int i = 0; i < count - 1; i++) {
				int a = idx(i), b = idx(i + 1);
				if (rt >= t[a] && rt <= t[b]) {
					float f = t[b] == t[a] ? 1f : (float) ((rt - t[a]) / (double) (t[b] - t[a]));
					set(st, a, b, f);
					return;
				}
			}
			set(st, n, n, 0f);
		}

		private void set(State st, int a, int b, float f) {
			st.x = Mth.lerp((double) f, x[a], x[b]);
			st.y = Mth.lerp((double) f, y[a], y[b]);
			st.z = Mth.lerp((double) f, z[a], z[b]);
			st.yRot = Mth.rotLerp(f, yaw[a], yaw[b]);
			st.xRot = Mth.lerp(f, pitch[a], pitch[b]);
		}
	}

	private static final Map<ThrowingDiscEntity, Smoother> SMOOTHERS = new WeakHashMap<>();

	@Override
	public void extractRenderState(ThrowingDiscEntity entity, State state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		// Server updates arrive a little unevenly, and the game just jumps to each one. So remember when each position
		// arrived and draw the disc about 1.5 ticks in the past, gliding between two real positions: smooth at any tick rate.
		Smoother sm = SMOOTHERS.computeIfAbsent(entity, e -> new Smoother());
		long now = System.nanoTime();
		sm.record(now, entity);
		float tps = Math.max(1f, entity.level().tickRateManager().tickrate());
		long delay = (long) (1.5 * 1.0e9 / tps);
		sm.sample(now - delay, state);
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
		// The disc's face points along its direction of travel (yaw + pitch, always the real heading, also after a
		// ricochet). It leans by the tilt (only at the turning point) and spins around the direction it is heading.
		Matrix4f m = new Matrix4f()
				.rotateY((float) Math.toRadians(-state.yRot))
				.rotateX((float) Math.toRadians(state.xRot))
				.rotateY((float) Math.toRadians(state.tilt))
				.rotateZ((float) Math.toRadians(state.spin));
		poseStack.mulPose(m);
		float sc = DiscConfig.renderScale;
		poseStack.scale(sc, sc, sc);
		// The item model is only one pixel thick, so from the side the disc looked like a flat sheet. Draw it several times,
		// stacked face to face (each layer is one model-thickness apart), so the disc has a real edge like a coin.
		int layers = DiscConfig.RENDER_LAYERS;
		float gap = 0.5f / 16f;   // one pixel of thickness after the item display scale
		for (int i = 0; i < layers; i++) {
			poseStack.pushPose();
			poseStack.translate(0f, 0f, (i - (layers - 1) / 2f) * gap);
			state.item.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
			poseStack.popPose();
		}
		poseStack.popPose();
	}
}
