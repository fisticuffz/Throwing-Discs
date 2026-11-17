package com.throwingdiscs;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class ThrowingDiscItem extends Item {
	public final DiscType type;

	public ThrowingDiscItem(DiscType type, Properties properties) {
		super(properties);
		this.type = type;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!level.isClientSide()) {
			Vec3 look = player.getLookAngle();
			ThrowingDiscEntity disc = new ThrowingDiscEntity(ModEntities.DISC, level);
			boolean offhand = hand == InteractionHand.OFF_HAND;
			int slot = offhand ? 40 : player.getInventory().getSelectedSlot();
			disc.launch(player, stack.copy(), type, offhand, slot, look);
			level.addFreshEntity(disc);
			level.playSound(null, player.getX(), player.getEyeY(), player.getZ(), SoundEvents.TRIDENT_THROW,
					SoundSource.PLAYERS, 0.9f, type.pitch * 1.5f);
			player.setItemInHand(hand, ItemStack.EMPTY);   // the disc is "in the air"; it comes back to this slot
		}
		return InteractionResult.SUCCESS;
	}
}
