package com.throwingdiscs;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Tuning values. config/throwing_discs.json is created on first start (and any new setting is added to it);
 * edit it and restart the game to change things, no rebuild needed.
 *
 * maxSpeedMultiplier : speed stops growing at this many times the disc's starting speed (0 = only HARD_SPEED_CAP applies)
 * ricochetSpeedBonus : each ricochet adds this much of the starting speed (0.2 = +20% per ricochet)
 * ricochetRangeBonus : each ricochet adds this many blocks of range
 * baseRange          : blocks it flies before it turns back
 * renderScale        : how big the disc is drawn (1.0 = 0.5 blocks wide, the size of its hitbox)
 * returnTilt         : degrees the disc leans when it reaches its end point and turns back (ricochets never tilt)
 * pickupDelayTicks   : the owner can touch the disc to take it back after this many ticks (20 = 1 second)
 * turnRate           : max degrees per tick the disc turns toward where it is going (higher = snappier)
 */
public final class DiscConfig {
	public static final double HARD_SPEED_CAP = 4.0;     // blocks per tick, safety limit
	public static final double RADIUS = 0.3;             // clearance kept from walls
	public static final int TURN_TICKS = 8;              // brake + turn phase length
	public static final int ACCEL_TICKS = 12;
	public static final double CATCH_DISTANCE = 0.2;     // caught right before it touches you
	public static final float FOLLOW_BASE = 0.45f;
	public static final float FOLLOW_BOOST = 0.65f;
	public static final int MAX_FLIGHT_TICKS = 1200;
	public static final int RENDER_LAYERS = 4;           // the disc is drawn this many pixels thick (1 = flat sheet)

	public static double maxSpeedMultiplier = 1.5;
	public static double ricochetSpeedBonus = 0.20;
	public static double ricochetRangeBonus = 1.0;
	public static double baseRange = 9.5;
	public static float renderScale = 1.3f;
	public static float returnTilt = 2.5f;
	public static int pickupDelayTicks = 20;
	public static float turnRate = 60.0f;

	private static class Data {
		double maxSpeedMultiplier = 1.5;
		double ricochetSpeedBonus = 0.20;
		double ricochetRangeBonus = 1.0;
		double baseRange = 9.5;
		float renderScale = 1.3f;
		float returnTilt = 2.5f;
		int pickupDelayTicks = 20;
		float turnRate = 60.0f;
	}

	public static void load() {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		Path file = FabricLoader.getInstance().getConfigDir().resolve("throwing_discs.json");
		try {
			Data d = new Data();
			if (Files.exists(file)) {
				Data read = gson.fromJson(Files.readString(file), Data.class);
				if (read != null) d = read;
			}
			maxSpeedMultiplier = d.maxSpeedMultiplier;
			ricochetSpeedBonus = d.ricochetSpeedBonus;
			ricochetRangeBonus = d.ricochetRangeBonus;
			baseRange = d.baseRange;
			renderScale = d.renderScale;
			returnTilt = d.returnTilt;
			pickupDelayTicks = d.pickupDelayTicks;
			turnRate = d.turnRate;
			Files.writeString(file, gson.toJson(d));   // writes any settings that were missing
		} catch (Exception e) {
			ThrowingDiscsMod.LOGGER.warn("Could not read throwing_discs.json, using defaults", e);
		}
	}

	public static double speedCap(DiscType t) {
		double cap = maxSpeedMultiplier <= 0 ? Double.MAX_VALUE : t.speed * maxSpeedMultiplier;
		return Math.min(cap, HARD_SPEED_CAP);
	}

	private DiscConfig() {
	}
}
