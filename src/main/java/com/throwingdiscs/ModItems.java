package com.throwingdiscs;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.EnumMap;
import java.util.Map;

public final class ModItems {
	private static final Map<DiscType, Item> ITEMS = new EnumMap<>(DiscType.class);

	public static Item get(DiscType type) {
		return ITEMS.get(type);
	}

	private static Item repairItem(DiscType t) {
		return switch (t) {
			case COPPER -> Items.COPPER_INGOT;
			case IRON -> Items.IRON_INGOT;
			case GOLD -> Items.GOLD_INGOT;
			case DIAMOND -> Items.DIAMOND;
			case NETHERITE -> Items.NETHERITE_INGOT;
		};
	}

	static {
		for (DiscType t : DiscType.values()) {
			ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, ThrowingDiscsMod.id(t.itemName()));
			Item.Properties props = new Item.Properties()
					.setId(key)
					.durability(t.durability)   // damageable items always stack to 1
					.enchantable(14)
					.repairable(repairItem(t));
			Item item = new ThrowingDiscItem(t, props);
			ITEMS.put(t, Registry.register(BuiltInRegistries.ITEM, key, item));
		}
		// Combat tab: right after the Trident (the other throwing weapon), next to the axes and mace.
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(tab -> {
			Item[] discs = new Item[DiscType.values().length];
			for (DiscType t : DiscType.values()) discs[t.ordinal()] = ITEMS.get(t);
			tab.insertAfter(Items.TRIDENT, discs);
		});
	}

	/** Call from the mod initializer to load this class. */
	public static void initialize() {
	}

	private ModItems() {
	}
}
