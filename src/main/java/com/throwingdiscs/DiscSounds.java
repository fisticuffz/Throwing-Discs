package com.throwingdiscs;

import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/** Spear sounds (vanilla ids). Change the ids here to change a sound. */
public final class DiscSounds {
	public static final SoundEvent RICOCHET = of("item.spear.hit");        // ricochet = spear pierce
	public static final SoundEvent HIT = of("item.spear.attack");          // disc hits a mob
	public static final SoundEvent RETURN = of("item.spear.lunge_1");      // caught / returned to the inventory
	// the throw sound is still SoundEvents.TRIDENT_THROW (see ThrowingDiscItem)

	private static SoundEvent of(String path) {
		return SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("minecraft", path));
	}

	private DiscSounds() {
	}
}
