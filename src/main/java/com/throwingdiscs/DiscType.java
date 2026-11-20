package com.throwingdiscs;

/** The five discs. speed = blocks per tick, damage = half hearts, pitch = sound pitch. */
public enum DiscType {
	COPPER("copper", 0.50, 5.0f, 190, 1.10f),
	IRON("iron", 0.55, 7.0f, 250, 1.00f),
	GOLD("gold", 0.60, 6.0f, 60, 1.35f),
	DIAMOND("diamond", 0.65, 8.0f, 1561, 1.20f),
	NETHERITE("netherite", 0.70, 9.0f, 2031, 0.80f);

	public final String id;
	public final double speed;
	public final float damage;
	public final int durability;
	public final float pitch;

	DiscType(String id, double speed, float damage, int durability, float pitch) {
		this.id = id;
		this.speed = speed;
		this.damage = damage;
		this.durability = durability;
		this.pitch = pitch;
	}

	public String itemName() {
		return id + "_disc";
	}
}
