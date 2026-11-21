package com.throwingdiscs;

import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DispenserBlock;

/** A dispenser throws the disc straight out; the disc flies, ricochets and comes back into the dispenser. */
public class DiscDispenseBehavior implements DispenseItemBehavior {
	private final DiscType type;

	public DiscDispenseBehavior(DiscType type) {
		this.type = type;
	}

	@Override
	public ItemStack dispense(BlockSource source, ItemStack stack) {
		ServerLevel level = source.level();
		Direction facing = source.state().getValue(DispenserBlock.FACING);
		ThrowingDiscEntity disc = new ThrowingDiscEntity(ModEntities.DISC, level);
		disc.launchFromDispenser(source.pos(), facing, stack.copy(), type);
		level.addFreshEntity(disc);
		level.playSound(null, source.pos().getX() + 0.5, source.pos().getY() + 0.5, source.pos().getZ() + 0.5,
				SoundEvents.TRIDENT_THROW, SoundSource.BLOCKS, 0.9f, type.pitch * 1.5f);
		return ItemStack.EMPTY;   // the disc is now in the air
	}
}
