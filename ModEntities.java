package com.throwingdiscs;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
	public static final ResourceKey<EntityType<?>> DISC_ID =
			ResourceKey.create(Registries.ENTITY_TYPE, ThrowingDiscsMod.id("disc"));

	public static final EntityType<ThrowingDiscEntity> DISC = Registry.register(
			BuiltInRegistries.ENTITY_TYPE, DISC_ID,
			EntityType.Builder.<ThrowingDiscEntity>of(ThrowingDiscEntity::new, MobCategory.MISC)
					.sized(0.5f, 0.15f)
					.clientTrackingRange(10)
					.updateInterval(1)
					.build(DISC_ID));

	/** Call from the mod initializer to load this class. */
	public static void initialize() {
	}

	private ModEntities() {
	}
}
