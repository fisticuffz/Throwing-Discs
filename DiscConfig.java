package com.throwingdiscs;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Tuning values. config/throwing_discs.json is created on first start.
 * maxSpeedMultiplier: 0   = speed keeps growing with every ricochet only HARD_SPEED_CAP applies)
 *                     1.5 = speed stops growing at 1.5x the disc's starting speed
 */
public final class DiscConfig {
	public static final double BASE_RANGE = 9.50;        // blocks before it returns
	public static final double RICOCHET_RANGE = 2.0;     // extra range per ricochet
	public static final double RICOCHET_SPEEDUP = 0.20;  // speed multiplier per ricochet
	public static final double HARD_SPEED_CAP = 5.0;     // blocks per tick, safety limit
	public static final double RADIUS = 0.3;             // clearance kept from walls
	public static final int TURN_TICKS = 8;              // brake + turn phase length
	public static final int ACCEL_TICKS = 12;
	public static final double CATCH_DISTANCE = 0.1;     // caught right before it touches you
	public static final float FOLLOW_BASE = 0.45f;       // facing speed while flying
	public static final float FOLLOW_BOOST = 0.65f;      // facing speed right after a ricochet
	public static final float RETURN_TILT = 4.0f;        // degrees, return trip only. Ricochets stay flat.
	public static final int MAX_FLIGHT_TICKS = 1200;

	public static double maxSpeedMultiplier = 1.5;

	private static class Data {
		double maxSpeedMultiplier = 1.5;
	}

	public static void load() {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		Path file = FabricLoader.getInstance().getConfigDir().resolve("throwing_discs.json");
		try {
			if (Files.exists(file)) {
				Data d = gson.fromJson(Files.readString(file), Data.class);
				if (d != null) maxSpeedMultiplier = d.maxSpeedMultiplier;
			} else {
				Files.writeString(file, gson.toJson(new Data()));
			}
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
