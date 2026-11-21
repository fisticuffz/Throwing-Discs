package com.throwingdiscs;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The flying disc.
 * OUT  : flies in a straight line, ricochets off hard blocks (real reflection), pierces mobs.
 * TURN : brakes hard, leans a little and swings round to face the owner.
 * BACK : accelerates to the owner and is caught right before it touches them.
 */
public class ThrowingDiscEntity extends Entity implements ItemSupplier {
	public static final EntityDataAccessor<ItemStack> DATA_ITEM =
			SynchedEntityData.defineId(ThrowingDiscEntity.class, EntityDataSerializers.ITEM_STACK);
	public static final EntityDataAccessor<Float> DATA_TILT =
			SynchedEntityData.defineId(ThrowingDiscEntity.class, EntityDataSerializers.FLOAT);

	private static final int OUT = 0, TURN = 1, BACK = 2;

	/** every disc currently in flight on the server, so they can be handed back when their owner leaves the world */
	private static final Set<ThrowingDiscEntity> ACTIVE = ConcurrentHashMap.newKeySet();

	/** Called when a player disconnects: every disc they have in the air goes back into their inventory. */
	public static void recallAll(Player player) {
		for (ThrowingDiscEntity d : new ArrayList<>(ACTIVE)) {
			if (!d.finished && player.getUUID().equals(d.ownerId) && d.level() instanceof ServerLevel sl) {
				d.finish(sl, player, false);
			}
		}
	}

	private UUID ownerId;
	private DiscType type = DiscType.IRON;
	private int phase = OUT, phaseTick, age, returnSlot = -1;
	private boolean offhand, finished;
	private boolean dispensed;      // thrown by a dispenser (no owner): it flies back to the dispenser
	private BlockPos home;
	private double flyMul = 1.0;    // Fly enchantment speed multiplier
	private int life;   // ticks since it was thrown (server)
	private double speed, traveled, range = DiscConfig.BASE_RANGE;
	private float follow = DiscConfig.FOLLOW_BASE, tilt, tiltSign = 1f;
	private final Map<Integer, Integer> hitCooldown = new HashMap<>();
	/** Flight velocity kept in our own field: vanilla's delta movement gets zeroed by /tp, which froze the disc. */
	private Vec3 flightVel = Vec3.ZERO;

	/** client only, for a spin animation in custom renderers */
	public float spin, spinO;

	/** client only: position/rotation at the previous and current client tick, so the renderer can blend between them
	 *  even when the game snaps the entity to each server update (this is what makes /tick rate 5 look smooth). */
	public double smX0, smY0, smZ0, smX1, smY1, smZ1;
	public float smYaw0, smYaw1, smPitch0, smPitch1;
	private boolean smoothInit;

	public ThrowingDiscEntity(EntityType<? extends ThrowingDiscEntity> entityType, Level level) {
		super(entityType, level);
		this.noPhysics = true;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(DATA_ITEM, ItemStack.EMPTY);
		builder.define(DATA_TILT, 0f);
	}

	@Override
	public ItemStack getItem() {
		return this.entityData.get(DATA_ITEM);
	}

	public float getTilt() {
		return this.entityData.get(DATA_TILT);
	}

	public void launch(Player owner, ItemStack stack, DiscType type, boolean offhand, int slot, Vec3 dir) {
		this.ownerId = owner.getUUID();
		ACTIVE.add(this);
		this.type = type;
		this.offhand = offhand;
		this.returnSlot = slot;
		applyEnchants(owner.level(), stack, type);
		this.entityData.set(DATA_ITEM, stack);
		Vec3 d = dir.normalize();
		setPos(owner.getEyePosition().add(d.scale(0.8)).add(0, -0.3, 0));
		setDeltaMovement(d.scale(speed));
		Vec3 facing = d.scale(-1);
		flightVel = d.scale(speed);
		setYRot((float) (Math.atan2(-d.x, d.z) * Mth.RAD_TO_DEG));
		setXRot((float) (-Math.asin(Mth.clamp(d.y, -1, 1)) * Mth.RAD_TO_DEG));
	}

	/** Thrown out of a dispenser: flies like a normal throw, then returns into the dispenser. */
	public void launchFromDispenser(BlockPos pos, Direction facing, ItemStack stack, DiscType type) {
		this.dispensed = true;
		this.home = pos;
		ACTIVE.add(this);
		this.type = type;
		applyEnchants(level(), stack, type);
		this.entityData.set(DATA_ITEM, stack);
		Vec3 d = Vec3.atLowerCornerOf(facing.getUnitVec3i()).normalize();
		setPos(Vec3.atCenterOf(pos).add(d.scale(0.7)));
		flightVel = d.scale(speed);
		setDeltaMovement(flightVel);
		setYRot((float) (Math.atan2(-d.x, d.z) * Mth.RAD_TO_DEG));
		setXRot((float) (-Math.asin(Mth.clamp(d.y, -1, 1)) * Mth.RAD_TO_DEG));
	}

	/** Fly = faster, Free Bird = longer range (both read from the disc's enchantments). */
	private void applyEnchants(Level level, ItemStack stack, DiscType type) {
		int fly = enchantLevel(level, stack, "fly");
		int free = enchantLevel(level, stack, "free_bird");
		this.flyMul = 1.0 + DiscConfig.FLY_SPEED_PER_LEVEL * fly;
		this.speed = type.speed * flyMul;
		this.range = DiscConfig.BASE_RANGE + DiscConfig.FREE_BIRD_RANGE_PER_LEVEL * free;
	}

	private static int enchantLevel(Level level, ItemStack stack, String path) {
		ResourceKey<net.minecraft.world.item.enchantment.Enchantment> key =
				ResourceKey.create(Registries.ENCHANTMENT, ThrowingDiscsMod.id(path));
		return level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(key)
				.map(h -> EnchantmentHelper.getItemEnchantmentLevel(h, stack)).orElse(0);
	}

	private void returnToDispenser(ServerLevel sl, ItemStack stack) {
		if (dispensed && home != null && sl.getBlockEntity(home) instanceof net.minecraft.world.Container c) {
			for (int i = 0; i < c.getContainerSize(); i++) {          // first empty slot of the dispenser
				if (c.getItem(i).isEmpty()) {
					c.setItem(i, stack);
					c.setChanged();
					return;
				}
			}
		}
		spawnAtLocation(sl, stack);
	}

	// ------------------------------------------------------------------ tick
	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide()) {
			spinO = spin;
			spin += 45f;
			if (!smoothInit) {
				smoothInit = true;
				smX1 = getX(); smY1 = getY(); smZ1 = getZ();
				smYaw1 = getYRot(); smPitch1 = getXRot();
			}
			smX0 = smX1; smY0 = smY1; smZ0 = smZ1;
			smYaw0 = smYaw1; smPitch0 = smPitch1;
			smX1 = getX(); smY1 = getY(); smZ1 = getZ();
			smYaw1 = getYRot(); smPitch1 = getXRot();
			return;
		}
		if (finished) return;
		ServerLevel sl = (ServerLevel) level();
		Player owner = ownerId == null ? null : sl.getPlayerByUUID(ownerId);
		if (!dispensed && (owner == null || owner.isRemoved() || owner.level() != level())) {
			finish(sl, null, false);
			return;
		}

		life++;
		if (phase != BACK && ownerTouches(owner, position(), position())) {   // you walked into it
			finish(sl, owner, true);
			return;
		}

		hitCooldown.replaceAll((k, v) -> v - 1);
		hitCooldown.values().removeIf(v -> v <= 0);
		if (phase == OUT && ++age > DiscConfig.MAX_FLIGHT_TICKS) startReturn();

		switch (phase) {
			case OUT -> flyOut(sl, owner);
			case TURN -> turn(sl, owner);
			default -> back(sl, owner);
		}
	}

	private void startReturn() {
		phase = TURN;
		phaseTick = 0;
		hitCooldown.clear();
		tiltSign = random.nextBoolean() ? 1f : -1f;
		follow = 0.3f;
	}

	// ------------------------------------------------------------------ outbound flight + ricochet
	private void flyOut(ServerLevel sl, Player owner) {
		Vec3 pos = position();
		Vec3 vel = flightVel;
		int n = Math.max(1, (int) Math.ceil(speed / 0.4));
		for (int i = 0; i < n && phase == OUT && !finished; i++) {
			Vec3 dir = vel.normalize();
			Vec3 step = dir.scale(speed / n);
			Vec3 to = pos.add(step).add(dir.scale(DiscConfig.RADIUS));
			BlockHitResult hit = sl.clip(new ClipContext(pos, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
			if (hit.getType() == HitResult.Type.BLOCK) {
				BlockPos bp = hit.getBlockPos();
				if (sl.getBlockState(bp).is(Blocks.DECORATED_POT)) {      // pots break and the disc comes home
					sl.destroyBlock(bp, true, owner);
					startReturn();
					break;
				}
				Vec3 normal = Vec3.atLowerCornerOf(hit.getDirection().getUnitVec3i());
				double d = vel.dot(normal);
				if (d < 0) vel = vel.subtract(normal.scale(2 * d));         // real reflection
				range += DiscConfig.RICOCHET_RANGE;
				speed = Math.min(speed * DiscConfig.RICOCHET_SPEEDUP, DiscConfig.speedCap(type) * flyMul);
				vel = vel.normalize().scale(speed);
				pos = hit.getLocation().add(normal.scale(DiscConfig.RADIUS + 0.01));
				follow = DiscConfig.FOLLOW_BOOST;
				hitCooldown.clear();
				snd(sl, DiscSounds.RICOCHET, 0.9f, type.pitch * (0.9f + random.nextFloat() * 0.3f));
				break;                                                       // one ricochet per tick: no zig-zag in corners
			}
			Vec3 prevPos = pos;
			pos = pos.add(step);
			traveled += step.length();
			setPos(pos);
			hitEntities(sl, owner, pos);
			if (finished) return;
			chop(sl, owner, pos);
			if (ownerTouches(owner, prevPos, pos)) {   // you ran into it mid-flight
				finish(sl, owner, true);
				return;
			}
			if (traveled >= range) startReturn();
		}
		setPos(pos);
		flightVel = vel;
		setDeltaMovement(vel);
		faceToward(vel, follow);
		follow = Math.max(DiscConfig.FOLLOW_BASE, follow * 0.93f);
		setTilt(Mth.lerp(0f, tilt, 0f));
	}

	// ------------------------------------------------------------------ return trip
	private Vec3 chest(Player owner) {
		if (owner == null) return Vec3.atCenterOf(home);   // dispensed: fly back to the dispenser
		return owner.position().add(0, owner.getBbHeight() * 0.6, 0);
	}

	private void turn(ServerLevel sl, Player owner) {
		Vec3 vel = flightVel.scale(0.65);
		flightVel = vel;
		setDeltaMovement(vel);
		Vec3 pos = position();
		if (vel.lengthSqr() > 1e-6) {
			Vec3 to = pos.add(vel).add(vel.normalize().scale(DiscConfig.RADIUS));
			boolean blocked = sl.clip(new ClipContext(pos, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getType() == HitResult.Type.BLOCK;
			if (!blocked) {
				pos = pos.add(vel);
				setPos(pos);
			}
		}
		hitEntities(sl, owner, pos);
		smashPots(sl, owner, pos);
		faceToward(chest(owner).subtract(pos), follow);
		setTilt(Mth.lerp(0f, tilt, DiscConfig.RETURN_TILT * tiltSign));
		if (++phaseTick >= DiscConfig.TURN_TICKS) {
			phase = BACK;
			phaseTick = 0;
		}
	}

	private void back(ServerLevel sl, Player owner) {
		Vec3 pos = position();
		Vec3 toTarget = chest(owner).subtract(pos);
		double dist = toTarget.length();
		if (dist <= (owner == null ? 0.7 : DiscConfig.CATCH_DISTANCE) || (owner != null && owner.getBoundingBox().inflate(0.1).contains(pos))) {
			finish(sl, owner, true);
			return;
		}
		double t = Math.min(1.0, phaseTick / (double) DiscConfig.ACCEL_TICKS);
		double spd = Mth.lerp(t, 0.12, type.speed * flyMul * 1.1);
		Vec3 dir = toTarget.normalize();
		pos = pos.add(dir.scale(Math.min(spd, dist)));
		setPos(pos);
		setDeltaMovement(dir.scale(spd));
		phaseTick++;
		hitEntities(sl, owner, pos);
		smashPots(sl, owner, pos);
		faceToward(dir, 0.4f);
		float k = dist > 5 ? 1f : dist > 2.5 ? 0.5f : 0f;                  // realigns while approaching
		setTilt(Mth.lerp(0.25f, tilt, DiscConfig.RETURN_TILT * tiltSign * k));
	}

	/** Return trip: smash pots and chop plants on the way (destroyBlock with no tool = normal drops, no silk touch). */
	private void smashPots(ServerLevel sl, Player owner, Vec3 pos) {
		BlockPos bp = BlockPos.containing(pos);
		if (sl.getBlockState(bp).is(Blocks.DECORATED_POT)) sl.destroyBlock(bp, true, owner);
		chop(sl, owner, pos);
	}

	/** The disc cuts crops and plants it flies through (they have no collision, so it passes straight through). */
	private void chop(ServerLevel sl, Player owner, Vec3 pos) {
		BlockPos min = BlockPos.containing(pos.add(-0.3, -0.3, -0.3));
		BlockPos max = BlockPos.containing(pos.add(0.3, 0.3, 0.3));
		for (BlockPos bp : BlockPos.betweenClosed(min, max)) {
			BlockState st = sl.getBlockState(bp);
			if (isChoppable(st)) sl.destroyBlock(bp.immutable(), true, owner);
		}
	}

	private static boolean isChoppable(BlockState st) {
		return st.is(BlockTags.CROPS) || st.is(BlockTags.FLOWERS)
				|| st.is(Blocks.SHORT_GRASS) || st.is(Blocks.TALL_GRASS) || st.is(Blocks.FERN) || st.is(Blocks.LARGE_FERN)
				|| st.is(Blocks.DEAD_BUSH) || st.is(Blocks.SUGAR_CANE);
	}

	/** True if the owner touches the disc (after it has been in the air for 1 second). Checks the whole path it just flew. */
	private boolean ownerTouches(Player owner, Vec3 from, Vec3 to) {
		if (owner == null || life < 20) return false;
		AABB box = owner.getBoundingBox().inflate(0.2);
		return box.contains(to) || box.clip(from, to).isPresent();
	}

	// ------------------------------------------------------------------ orientation
	private void faceToward(Vec3 dir, float f) {
		if (dir.lengthSqr() < 1e-6) return;
		float ty = (float) (Math.atan2(dir.x, -dir.z) * Mth.RAD_TO_DEG);
		float tp = (float) (-Math.asin(Mth.clamp(dir.y / dir.length(), -1, 1)) * Mth.RAD_TO_DEG);
		// turn toward the direction of travel at a steady speed (no lag, no overshoot): exact while flying straight,
		// and a bounce / the return turn swings round smoothly instead of snapping
		float max = DiscConfig.TURN_RATE;
		float dy = Mth.wrapDegrees(ty - getYRot());
		float dp = Mth.wrapDegrees(tp - getXRot());
		setYRot(getYRot() + Mth.clamp(dy, -max, max));
		setXRot(getXRot() + Mth.clamp(dp, -max, max));
	}

	private void setTilt(float t) {
		tilt = t;
		entityData.set(DATA_TILT, t);
	}

	// ------------------------------------------------------------------ hitting things (pierces)
	private void hitEntities(ServerLevel sl, Player owner, Vec3 pos) {
		AABB box = new AABB(pos, pos).inflate(0.4, 0.3, 0.4);
		for (Entity e : sl.getEntities(this, box, x -> x instanceof LivingEntity le && le.isAlive() && x != owner
				&& !(x instanceof ThrowingDiscEntity) && !hitCooldown.containsKey(x.getId()))) {
			hitCooldown.put(e.getId(), 12);
			ItemStack stack = getItem();
			DamageSource src = sl.damageSources().thrown(this, owner);
			float dmg = EnchantmentHelper.modifyDamage(sl, stack, e, src, type.damage);   // Sharpness etc.
			e.hurtServer(sl, src, dmg);
			EnchantmentHelper.doPostAttackEffectsWithItemSource(sl, e, src, stack);       // Fire Aspect etc.
			snd(sl, DiscSounds.HIT, 0.8f, type.pitch * 1.2f);
			wear(sl, owner);
			if (finished) return;
		}
	}

	private void wear(ServerLevel sl, Player owner) {
		ItemStack stack = getItem().copy();
		boolean[] broke = {false};
		stack.hurtAndBreak(1, sl, owner instanceof ServerPlayer sp ? sp : null, item -> broke[0] = true);   // honours Unbreaking
		entityData.set(DATA_ITEM, stack);
		if (broke[0] || stack.isEmpty()) {
			snd(sl, SoundEvents.ITEM_BREAK, 0.9f, 1f);
			finished = true;
			discard();
		}
	}

	// ------------------------------------------------------------------ catching / giving back
	private void finish(ServerLevel sl, Player owner, boolean caught) {
		if (finished) return;
		finished = true;
		ItemStack stack = getItem().copy();
		if (owner == null) {
			returnToDispenser(sl, stack);
		} else {
			boolean placed = false;
			if (offhand) {
				if (owner.getOffhandItem().isEmpty()) {
					owner.setItemInHand(InteractionHand.OFF_HAND, stack);
					placed = true;
				}
			} else if (returnSlot >= 0 && owner.getInventory().getItem(returnSlot).isEmpty()) {
				owner.getInventory().setItem(returnSlot, stack);
				placed = true;
			}
			if (!placed && !owner.getInventory().add(stack)) owner.spawnAtLocation(sl, stack);
			if (caught) snd(sl, DiscSounds.RETURN, 0.7f, type.pitch * 1.1f);
		}
		discard();
	}

	@Override
	public void remove(RemovalReason reason) {
		ACTIVE.remove(this);
		// chunk unload / world close while flying: hand the disc back instead of losing it
		if (!level().isClientSide() && !finished && reason != RemovalReason.DISCARDED && reason != RemovalReason.KILLED) {
			finished = true;
			ItemStack stack = getItem().copy();
			if (level() instanceof ServerLevel sl) {
				Player owner = ownerId == null ? null : sl.getPlayerByUUID(ownerId);
				if (owner == null) returnToDispenser(sl, stack);
				else if (!owner.getInventory().add(stack)) owner.spawnAtLocation(sl, stack);
			}
		}
		super.remove(reason);
	}

	private void snd(ServerLevel sl, SoundEvent ev, float vol, float pitch) {
		sl.playSound(null, getX(), getY(), getZ(), ev, SoundSource.PLAYERS, vol, pitch);
	}

	private void snd(ServerLevel sl, Holder<SoundEvent> ev, float vol, float pitch) {
		sl.playSound(null, getX(), getY(), getZ(), ev, SoundSource.PLAYERS, vol, pitch);
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		return false;   // the disc entity is immortal; only the item's durability can end it
	}

	@Override
	public boolean shouldBeSaved() {
		return false;
	}

	@Override
	public boolean isPickable() {
		return false;
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
	}
}
