package com.throwingdiscs;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The flying disc.
 * OUT  : flies in a straight line, ricochets off hard blocks (real reflection), pierces mobs.
 * TURN : brakes hard, leans a little and swings round to face the owner.
 * BACK : accelerates to the owner and is caught right before it touches them.
 * After 1 second in the air the owner can also catch it simply by touching it.
 *
 * The server moves the disc and tells clients its direction (DATA_YAW / DATA_PITCH) and lean (DATA_TILT).
 * The client turns the disc toward that direction smoothly and spins it (see ThrowingDiscRenderer).
 */
public class ThrowingDiscEntity extends Entity implements ItemSupplier {
	public static final EntityDataAccessor<ItemStack> DATA_ITEM =
			SynchedEntityData.defineId(ThrowingDiscEntity.class, EntityDataSerializers.ITEM_STACK);
	public static final EntityDataAccessor<Float> DATA_TILT =
			SynchedEntityData.defineId(ThrowingDiscEntity.class, EntityDataSerializers.FLOAT);
	public static final EntityDataAccessor<Float> DATA_YAW =
			SynchedEntityData.defineId(ThrowingDiscEntity.class, EntityDataSerializers.FLOAT);
	public static final EntityDataAccessor<Float> DATA_PITCH =
			SynchedEntityData.defineId(ThrowingDiscEntity.class, EntityDataSerializers.FLOAT);

	private static final int OUT = 0, TURN = 1, BACK = 2;
	private static final int TOUCH_CATCH_AFTER_TICKS = 20;   // 1 second

	// ---- server state
	private UUID ownerId;
	private DiscType type = DiscType.IRON;
	private int phase = OUT, phaseTick, life, returnSlot = -1;
	private boolean offhand, finished;
	private double speed, traveled, range = DiscConfig.BASE_RANGE;
	private float tilt, tiltSign = 1f;
	private double lastSweepDist;
	private final Map<Integer, Integer> hitCooldown = new HashMap<>();

	// ---- client state (visual only)
	public float spin, spinO;
	public float viewYaw, viewYawO, viewPitch, viewPitchO;
	private float viewFollow = DiscConfig.FOLLOW_BASE;
	private boolean viewInit;

	public ThrowingDiscEntity(EntityType<? extends ThrowingDiscEntity> entityType, Level level) {
		super(entityType, level);
		this.noPhysics = true;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(DATA_ITEM, ItemStack.EMPTY);
		builder.define(DATA_TILT, 0f);
		builder.define(DATA_YAW, 0f);
		builder.define(DATA_PITCH, 0f);
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
		this.type = type;
		this.offhand = offhand;
		this.returnSlot = slot;
		this.speed = type.speed;
		this.entityData.set(DATA_ITEM, stack);
		Vec3 d = dir.normalize();
		setPos(owner.getEyePosition().add(d.scale(0.8)).add(0, -0.3, 0));
		setDeltaMovement(d.scale(speed));
		lookAlong(d);
	}

	// ------------------------------------------------------------------ tick
	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide()) {
			clientTick();
			return;
		}
		if (finished) return;
		ServerLevel sl = (ServerLevel) level();
		Player owner = ownerId == null ? null : sl.getPlayerByUUID(ownerId);
		if (owner == null || owner.isRemoved() || owner.level() != level()) {
			finish(sl, null, false);
			return;
		}
		life++;
		if (ownerTouches(owner, position(), position())) {       // you walked into it
			finish(sl, owner, true);
			return;
		}

		hitCooldown.replaceAll((k, v) -> v - 1);
		hitCooldown.values().removeIf(v -> v <= 0);
		if (phase == OUT && life > DiscConfig.MAX_FLIGHT_TICKS) startReturn();

		switch (phase) {
			case OUT -> flyOut(sl, owner);
			case TURN -> turn(sl, owner);
			default -> back(sl, owner);
		}
	}

	/** Visual only: spin, and turn toward the direction the server says we are flying. */
	private void clientTick() {
		spinO = spin;
		spin += 45f;                                              // 900 degrees per second
		float ty = entityData.get(DATA_YAW);
		float tp = entityData.get(DATA_PITCH);
		if (!viewInit) {                                          // thrown: look exactly where it is thrown
			viewYaw = ty;
			viewPitch = tp;
			viewInit = true;
		}
		viewYawO = viewYaw;
		viewPitchO = viewPitch;
		float change = Math.abs(Mth.wrapDegrees(ty - viewYaw)) + Math.abs(tp - viewPitch);
		if (change > 25f) viewFollow = DiscConfig.FOLLOW_BOOST;  // ricochet / turn: swing round quicker
		viewYaw = Mth.rotLerp(viewFollow, viewYaw, ty);
		viewPitch = Mth.lerp(viewFollow, viewPitch, tp);
		viewFollow = Math.max(DiscConfig.FOLLOW_BASE, viewFollow * 0.93f);
	}

	private void startReturn() {
		phase = TURN;
		phaseTick = 0;
		hitCooldown.clear();
		tiltSign = random.nextBoolean() ? 1f : -1f;
	}

	private void lookAlong(Vec3 d) {
		if (d.lengthSqr() < 1e-8) return;
		entityData.set(DATA_YAW, (float) (Math.atan2(-d.x, d.z) * Mth.RAD_TO_DEG));
		entityData.set(DATA_PITCH, (float) (-Math.asin(Mth.clamp(d.y / d.length(), -1, 1)) * Mth.RAD_TO_DEG));
	}

	// ------------------------------------------------------------------ catching by touch
	private boolean ownerTouches(Player owner, Vec3 from, Vec3 to) {
		if (life < TOUCH_CATCH_AFTER_TICKS) return false;
		AABB box = owner.getBoundingBox().inflate(0.3);
		return box.contains(to) || box.clip(from, to).isPresent();
	}

	// ------------------------------------------------------------------ block sweep (centre ray + 4 side rays)
	private BlockHitResult sweep(ServerLevel sl, Vec3 from, Vec3 dir, double dist) {
		Vec3 to = from.add(dir.scale(dist));
		BlockHitResult best = sl.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
		lastSweepDist = best.getType() == HitResult.Type.BLOCK ? best.getLocation().distanceTo(from) : Double.MAX_VALUE;
		Vec3 up = Math.abs(dir.y) > 0.99 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
		Vec3 a = dir.cross(up).normalize().scale(DiscConfig.RADIUS * 0.8);
		Vec3 b = dir.cross(a).normalize().scale(DiscConfig.RADIUS * 0.8);
		for (Vec3 off : new Vec3[]{a, a.scale(-1), b, b.scale(-1)}) {
			Vec3 o = from.add(off);
			BlockHitResult h = sl.clip(new ClipContext(o, to.add(off), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
			if (h.getType() == HitResult.Type.BLOCK) {
				double d = h.getLocation().distanceTo(o);
				if (d < lastSweepDist) {
					lastSweepDist = d;
					best = h;
				}
			}
		}
		return best;
	}

	// ------------------------------------------------------------------ outbound flight + ricochet
	private void flyOut(ServerLevel sl, Player owner) {
		Vec3 pos = position();
		Vec3 vel = getDeltaMovement();
		if (vel.lengthSqr() < 1e-8) vel = new Vec3(0, 0, 1).scale(speed);
		double remaining = speed;
		int bounces = 0;
		while (remaining > 1e-4 && phase == OUT && !finished) {
			Vec3 dir = vel.normalize();
			double step = Math.min(remaining, 0.4);
			BlockHitResult hit = sweep(sl, pos, dir, step + DiscConfig.RADIUS);
			if (hit.getType() == HitResult.Type.BLOCK) {
				BlockPos bp = hit.getBlockPos();
				if (sl.getBlockState(bp).is(Blocks.DECORATED_POT)) {      // pots break and the disc comes home
					sl.destroyBlock(bp, true, owner);
					startReturn();
					break;
				}
				Vec3 normal = Vec3.atLowerCornerOf(hit.getDirection().getUnitVec3i());
				double d = dir.dot(normal);
				if (d < 0) dir = dir.subtract(normal.scale(2 * d));         // real reflection off the face
				double adv = Math.max(0.0, Math.min(step, lastSweepDist - DiscConfig.RADIUS));
				pos = pos.add(vel.normalize().scale(adv));                  // stop just short of the wall
				range += DiscConfig.RICOCHET_RANGE;
				speed = Math.min(speed * DiscConfig.RICOCHET_SPEEDUP, DiscConfig.speedCap(type));
				vel = dir.normalize().scale(speed);
				hitCooldown.clear();
				snd(sl, SoundEvents.TRIDENT_HIT_GROUND, 0.8f, type.pitch * (0.9f + random.nextFloat() * 0.3f));
				snd(sl, SoundEvents.ANVIL_LAND, 0.15f, 1.8f);
				remaining -= Math.max(adv, 0.1);
				if (++bounces > 6) {                                        // wedged in a tight spot
					startReturn();
					break;
				}
				continue;
			}
			Vec3 prev = pos;
			pos = pos.add(dir.scale(step));
			traveled += step;
			remaining -= step;
			setPos(pos);
			hitEntities(sl, owner, pos);
			if (finished) return;
			if (ownerTouches(owner, prev, pos)) {
				finish(sl, owner, true);
				return;
			}
			if (traveled >= range) startReturn();
		}
		setPos(pos);
		setDeltaMovement(vel);
		lookAlong(vel);                       // flat on walls, pitched along the path after a floor/ceiling bounce
		setTilt(Mth.lerp(0.35f, tilt, 0f));   // ricochets never lean the disc
	}

	// ------------------------------------------------------------------ return trip
	private Vec3 chest(Player owner) {
		return owner.position().add(0, owner.getBbHeight() * 0.6, 0);
	}

	private void turn(ServerLevel sl, Player owner) {
		Vec3 vel = getDeltaMovement().scale(0.65);
		setDeltaMovement(vel);
		Vec3 pos = position();
		if (vel.lengthSqr() > 1e-6) {
			BlockHitResult hit = sweep(sl, pos, vel.normalize(), vel.length() + DiscConfig.RADIUS);
			if (hit.getType() != HitResult.Type.BLOCK) {
				Vec3 prev = pos;
				pos = pos.add(vel);
				setPos(pos);
				if (ownerTouches(owner, prev, pos)) {
					finish(sl, owner, true);
					return;
				}
			}
		}
		hitEntities(sl, owner, pos);
		smashPots(sl, owner, pos);
		lookAlong(chest(owner).subtract(pos));
		setTilt(Mth.lerp(0.45f, tilt, DiscConfig.RETURN_TILT * tiltSign));   // the slight tilt before it returns
		if (++phaseTick >= DiscConfig.TURN_TICKS) {
			phase = BACK;
			phaseTick = 0;
		}
	}

	private void back(ServerLevel sl, Player owner) {
		Vec3 pos = position();
		Vec3 toTarget = chest(owner).subtract(pos);
		double dist = toTarget.length();
		if (dist <= DiscConfig.CATCH_DISTANCE || owner.getBoundingBox().inflate(0.1).contains(pos)) {
			finish(sl, owner, true);
			return;
		}
		double t = Math.min(1.0, phaseTick / (double) DiscConfig.ACCEL_TICKS);
		double spd = Mth.lerp(t, 0.12, type.speed * 1.1);
		Vec3 dir = toTarget.normalize();
		pos = pos.add(dir.scale(Math.min(spd, dist)));
		setPos(pos);
		setDeltaMovement(dir.scale(spd));
		phaseTick++;
		hitEntities(sl, owner, pos);
		smashPots(sl, owner, pos);
		lookAlong(dir);
		float k = dist > 5 ? 1f : dist > 2.5 ? 0.5f : 0f;                  // realigns while approaching
		setTilt(Mth.lerp(0.25f, tilt, DiscConfig.RETURN_TILT * tiltSign * k));
	}

	private void smashPots(ServerLevel sl, Player owner, Vec3 pos) {
		BlockPos bp = BlockPos.containing(pos);
		if (sl.getBlockState(bp).is(Blocks.DECORATED_POT)) sl.destroyBlock(bp, true, owner);
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
			snd(sl, SoundEvents.TRIDENT_HIT, 0.8f, type.pitch * 1.2f);
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
			spawnAtLocation(sl, stack);
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
			if (!placed && !owner.getInventory().add(stack)) owner.drop(stack, false);
			if (caught) snd(sl, SoundEvents.TRIDENT_RETURN, 0.7f, type.pitch * 1.2f);
		}
		discard();
	}

	@Override
	public void remove(RemovalReason reason) {
		// chunk unload / world close while flying: hand the disc back instead of losing it
		if (!level().isClientSide() && !finished && reason != RemovalReason.DISCARDED && reason != RemovalReason.KILLED) {
			finished = true;
			ItemStack stack = getItem().copy();
			if (level() instanceof ServerLevel sl) {
				Player owner = ownerId == null ? null : sl.getPlayerByUUID(ownerId);
				if (owner != null && !owner.getInventory().add(stack)) owner.drop(stack, false);
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
