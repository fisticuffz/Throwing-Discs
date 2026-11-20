// Throwing Discs - boomerang discs with ricochet physics.
// Flow per disc:  OUT (flies, ricochets off blocks, pierces mobs)
//                 -> TURN (slows quickly, tilts, swings to face the player)
//                 -> BACK (accelerates to the player, realigns, gets caught)
import { world, system, ItemStack, EquipmentSlot } from "@minecraft/server";

// ---------------------------------------------------------------- CONFIG
const DISCS = {
  "ore_discs:copper_disc":    { ent: "ore_discs:copper_disc_entity", damage: 5, speed: 0.55 , snd: 1.1 },
  "ore_discs:iron_disc":      { ent: "ore_discs:iron_disc_entity", damage: 7, speed: 0.6 , snd: 1.0 },
  "ore_discs:gold_disc":      { ent: "ore_discs:gold_disc_entity", damage: 6, speed: 0.7, snd: 1.35 }, // fast but fragile (60 uses)
  "ore_discs:diamond_disc":   { ent: "ore_discs:diamond_disc_entity", damage: 8, speed: 0.65 , snd: 1.2 },
  "ore_discs:netherite_disc": { ent: "ore_discs:netherite_disc_entity", damage: 9, speed: 0.7 , snd: 0.8 },
};
const CFG = {
  baseRange: 9.5,        // blocks travelled before returning
  ricochetBonus: 2.0,     // extra range granted by every ricochet
  turnTicks: 8,         // length of the "slow down + tilt + face player" phase
  accelTicks: 12,       // ticks to reach full speed on the way back
  catchDistance: 0.1,      // caught right before it touches you
  maxLifetime: 1200,     // safety: ticks before a disc is forced home
  hitRadius: 0.9,
  discRadius: 0.3,      // clearance kept from walls
  maxTilt: 4,           // lean while returning (small)
  followBase: 0.25,     // how fast it turns to face its travel direction while flying (0-1 per tick)
  followBoost: 0.65,    // ...right after a ricochet (fades back to followBase)
  wallBank: 0,          // lean after bouncing off a WALL (0 = stays flat)
  floorBank: 0,         // lean after bouncing off the ground / a ceiling (0 = stays flat, try 24 for a lean)
  throwCooldown: 6,
  ricochetSpeedUp: 0.2, // each ricochet makes it faster...
  maxSpeedMul: 1.1,      // speed increases with every ricochet up to 1.5x its starting speed
  hardSpeedCap: 5,      // technical safety limit (blocks per tick)
};
const NORMALS = {
  Up: { x: 0, y: 1, z: 0 }, Down: { x: 0, y: -1, z: 0 },
  North: { x: 0, y: 0, z: -1 }, South: { x: 0, y: 0, z: 1 },
  East: { x: 1, y: 0, z: 0 }, West: { x: -1, y: 0, z: 0 },
};
const POTS = new Set(["minecraft:decorated_pot"]);

// ---------------------------------------------------------------- MATH
const add = (a, b) => ({ x: a.x + b.x, y: a.y + b.y, z: a.z + b.z });
const sub = (a, b) => ({ x: a.x - b.x, y: a.y - b.y, z: a.z - b.z });
const mul = (a, s) => ({ x: a.x * s, y: a.y * s, z: a.z * s });
const dot = (a, b) => a.x * b.x + a.y * b.y + a.z * b.z;
const len = (a) => Math.hypot(a.x, a.y, a.z);
const norm = (a) => { const l = len(a) || 1; return mul(a, 1 / l); };
const clamp = (v, lo, hi) => Math.max(lo, Math.min(hi, v));
const lerp = (a, b, t) => a + (b - a) * t;
const wrap = (a) => ((((a + 180) % 360) + 360) % 360) - 180;
const angleDiff = (from, to) => wrap(to - from);
const lerpAngle = (a, b, t) => wrap(a + angleDiff(a, b) * t);
const yawOf = (d) => (Math.atan2(-d.x, d.z) * 180) / Math.PI;
const pitchOf = (d) => (-Math.asin(clamp(d.y / (len(d) || 1), -1, 1)) * 180) / Math.PI;
const easeOut = (t) => 1 - (1 - t) * (1 - t);
const isValid = (e) => { try { return typeof e.isValid === "function" ? e.isValid() : e.isValid; } catch { return false; } };

let lastReport = -999;
function report(where, e) {
  console.warn(`[Discs] ${where}: ${e} ${e?.stack ?? ""}`);
  if (system.currentTick - lastReport < 100) return;
  lastReport = system.currentTick;
  try { world.sendMessage(`§c[Discs] error in ${where}: ${e}`); } catch {}
}
system.runTimeout(() => { try { world.sendMessage("§a[Discs] Throwing Discs script loaded."); } catch {} }, 60);

// ---------------------------------------------------------------- STATE
const discs = new Map();        // entity id -> state
const cooldown = new Map();     // player id -> tick

function getPlayer(id) { return world.getAllPlayers().find((p) => p.id === id); }
function snd(dim, id, loc, volume = 1, pitch = 1) { try { dim.playSound(id, loc, { volume, pitch }); } catch {} }

// orientation is sent to the client through synced entity values:
//   mark_variant = bank/tilt (2 degree steps, 25 levels = +-24 degrees), variant = pitch (3 degree steps, 61 levels)
function setTilt(s) {
  const tl = clamp(Math.round(s.tilt / 2) + 12, 0, 24);
  const pl = clamp(Math.round(s.pitch / 3) + 30, 0, 60);
  try {
    if (tl !== s.tiltLvl) { s.tiltLvl = tl; s.entity.triggerEvent(`ore_discs:tilt_${tl}`); }
    if (pl !== s.pitchLvl) { s.pitchLvl = pl; s.entity.triggerEvent(`ore_discs:pitch_${pl}`); }
  } catch (e) { report("orientation event", e); }
}

// ---------------------------------------------------------------- THROW
world.afterEvents.itemUse.subscribe((ev) => { try { onUse(ev); } catch (e) { report("throw", e); } });
function onUse(ev) {
  const cfg = DISCS[ev.itemStack?.typeId];
  if (!cfg) return;
  const player = ev.source;
  const now = system.currentTick;
  if (now - (cooldown.get(player.id) ?? -99) < CFG.throwCooldown) return;

  const slot = player.getComponent("minecraft:equippable")?.getEquipmentSlot(EquipmentSlot.Mainhand);
  const item = slot?.getItem();
  if (!item || item.typeId !== ev.itemStack.typeId) return;
  cooldown.set(player.id, now);

  // enchantments: sharpness adds damage, fire aspect sets targets on fire
  let bonus = 0, fire = 0, unb = 0;
  const dur = item.getComponent("minecraft:durability");
  try {
    for (const en of item.getComponent("minecraft:enchantable")?.getEnchantments() ?? []) {
      const id = String(en.type?.id ?? en.type);
      if (id.includes("sharpness")) bonus += 0.5 * en.level + 0.5;
      if (id.includes("fire_aspect")) fire = Math.max(fire, en.level * 4);
      if (id.includes("unbreaking")) unb = en.level;
    }
  } catch {}

  const dir = norm(player.getViewDirection());
  const start = add(player.getHeadLocation(), add(mul(dir, 0.8), { x: 0, y: -0.3, z: 0 }));
  const entity = player.dimension.spawnEntity(cfg.ent, start);
  const yaw = yawOf(dir), pitch = pitchOf(dir);
  slot.setItem(undefined);
  discs.set(entity.id, {
    id: entity.id, entity, ownerId: player.id, item: item.clone(), cfg, phase: "out", slotIndex: player.selectedSlotIndex,
    dir, speed: cfg.speed, traveled: 0, range: CFG.baseRange, ricochets: 0,
    yaw, pitch, tilt: 0, phaseTick: 0, age: 0, hit: new Set(), bonus, fire, unb, tiltSign: 1, done: false,
    dmg: dur?.damage ?? 0, maxDur: dur?.maxDurability ?? Infinity,
  });
  const st = discs.get(entity.id);
  try { entity.teleport(start, { rotation: { x: pitch, y: yaw } }); } catch {}
  setTilt(st);                                            // face the throw direction from the very first frame
  try {
    entity.setDynamicProperty("owner", player.id);
    entity.setDynamicProperty("item", item.typeId);
  } catch (e) { report("dynamic property", e); }
  snd(player.dimension, "item.trident.throw", start, 0.9, cfg.snd * 1.5);
}

// ---------------------------------------------------------------- HITS
function hitEntities(s, dim, pos) {
  let near;
  try { near = dim.getEntities({ location: pos, maxDistance: 3, excludeFamilies: ["ore_disc"] }); } catch { return; }
  const owner = getPlayer(s.ownerId);
  for (const e of near) {
    if (e.id === s.ownerId || s.hit.has(e.id) || !isValid(e)) continue;
    const hp = e.getComponent("minecraft:health");
    if (!hp || hp.currentValue <= 0) continue;
    const dx = e.location.x - pos.x, dz = e.location.z - pos.z;
    if (Math.hypot(dx, dz) > CFG.hitRadius) continue;
    let top = 1.0;
    try { top = Math.max(0.6, e.getHeadLocation().y - e.location.y + 0.3); } catch {}
    if (pos.y < e.location.y - 0.4 || pos.y > e.location.y + top) continue;
    s.hit.add(e.id);
    try { e.applyDamage(s.cfg.damage + (s.bonus ?? 0), { cause: "entityAttack", damagingEntity: owner }); } catch {}
    snd(dim, "item.trident.hit", e.location, 0.8, s.cfg.snd * 1.2);
    if (s.fire) { try { e.setOnFire(s.fire, true); } catch {} }
    if (!(s.unb && Math.random() > 1 / (s.unb + 1))) s.dmg++;          // Unbreaking
    if (s.dmg >= s.maxDur) { breakDisc(s, dim); return; }
  }
}

function breakDisc(s, dim) {
  if (s.done) return;
  s.done = true;
  discs.delete(s.id);
  snd(dim, "random.break", s.entity.location, 0.9, 1);
  killDisc(s.entity);
}

// remove an entity for sure (remove -> despawn event -> kill)
function killDisc(e) {
  try { e.remove(); } catch {}
  try { if (isValid(e)) e.triggerEvent("ore_discs:despawn"); } catch {}
  try { if (isValid(e)) e.kill(); } catch {}
}

// ---------------------------------------------------------------- PHASES
function startReturn(s) {
  s.phase = "turn"; s.phaseTick = 0; s.hit.clear(); s.turnSpeed = s.speed;
  s.tiltSign = Math.random() < 0.5 ? -1 : 1;
}

// ---- block collision (own voxel test, no raycast API)
function blockAt(dim, x, y, z) {
  try { return dim.getBlock({ x: Math.floor(x), y: Math.floor(y), z: Math.floor(z) }); } catch { return undefined; }
}
const PASSABLE = new Set(["air","cave_air","void_air","structure_void","light_block","grass","short_grass","tall_grass","tallgrass","fern","large_fern","double_plant","deadbush","dead_bush","short_dry_grass","tall_dry_grass","yellow_flower","red_flower","dandelion","poppy","torchflower","pitcher_plant","spore_blossom","torch","redstone_torch","soul_torch","copper_torch","rail","golden_rail","detector_rail","activator_rail","web","vine","weeping_vines","twisting_vines","cave_vines","glow_lichen","sculk_vein","ladder","snow_layer","seagrass","kelp","reeds","sugar_cane","wheat","carrots","potatoes","beetroot","sweet_berry_bush","fire","soul_fire","redstone_wire","tripwire","tripwire_hook","lever","hanging_roots","leaf_litter","wildflowers","firefly_bush","lily_pad","frogspawn","nether_sprouts","bamboo_sapling","brown_mushroom","red_mushroom","crimson_fungus","warped_fungus","crimson_roots","warped_roots","sapling","azalea_bush"]);
const PASSABLE_SUFFIX = /(_sapling|_torch|_button|_sign|_rail|_carpet|_fan|_pressure_plate|_banner|_candle|_roots|_petals|_tulip|_orchid|_cornflower|_daisy|_allium|_bluet|_lily_of_the_valley|_flower)$/;
function flag(b, name) { try { const v = b[name]; return typeof v === "function" ? v.call(b) : !!v; } catch { return false; } }
function isSolidBlock(b) {
  if (!b) return false;
  if (POTS.has(b.typeId)) return true;
  if (flag(b, "isAir") || flag(b, "isLiquid")) return false;
  const name = String(b.typeId).replace("minecraft:", "");
  if (PASSABLE.has(name) || PASSABLE_SUFFIX.test(name)) return false;
  return true;
}
const RX = CFG.discRadius, RY = 0.2;

function stepOut(s, dim) {
  let pos = s.entity.location;
  let remaining = s.speed;
  while (remaining > 1e-4 && s.phase === "out") {
    const step = Math.min(remaining, 0.4);
    const d = s.dir;
    const nx = pos.x + d.x * step, ny = pos.y + d.y * step, nz = pos.z + d.z * step;
    const sx = Math.sign(d.x), sy = Math.sign(d.y), sz = Math.sign(d.z);

    const bx = sx !== 0 ? blockAt(dim, nx + sx * RX, pos.y, pos.z) : undefined;
    const by = sy !== 0 ? blockAt(dim, pos.x, ny + sy * RY, pos.z) : undefined;
    const bz = sz !== 0 ? blockAt(dim, pos.x, pos.y, nz + sz * RX) : undefined;
    let fx = isSolidBlock(bx), fy = isSolidBlock(by), fz = isSolidBlock(bz);
    let bc;
    if (!fx && !fy && !fz) {              // corner hit
      bc = blockAt(dim, nx + sx * RX, ny + sy * RY, nz + sz * RX);
      if (isSolidBlock(bc)) { fx = fy = fz = true; }
    }

    if (fx || fy || fz) {
      const hitBlock = [bx, by, bz, bc].find((b) => b && POTS.has(b.typeId));
      if (hitBlock) {                     // pottery: smash it and come home
        try { dim.runCommand(`setblock ${hitBlock.x} ${hitBlock.y} ${hitBlock.z} air destroy`); } catch {}
        snd(dim, "dig.stone", pos, 1.0, 0.8);
        startReturn(s);
        break;
      }
      const old = d;
      s.dir = norm({ x: fx ? -d.x : d.x, y: fy ? -d.y : d.y, z: fz ? -d.z : d.z });  // ricochet
      s.follow = CFG.followBoost;
      s.yaw = lerpAngle(s.yaw, yawOf(s.dir), 0.5);
      s.pitch = lerp(s.pitch, pitchOf(s.dir), 0.6);
      const cross = old.x * s.dir.z - old.z * s.dir.x;               // which way it turned
      const side = Math.abs(cross) > 0.05 ? Math.sign(cross) : (Math.random() < 0.5 ? -1 : 1);
      s.kick = side * ((fx || fz) ? CFG.wallBank : CFG.floorBank);   // walls: flat, floor/ceiling: lean
      s.ricochets++;
      s.range += CFG.ricochetBonus;
      s.speed = Math.min(s.speed * CFG.ricochetSpeedUp, s.cfg.speed * CFG.maxSpeedMul, CFG.hardSpeedCap);
      s.hit.clear();
      snd(dim, "item.trident.hit_ground", pos, 0.8, s.cfg.snd * (0.9 + Math.random() * 0.3));
      snd(dim, "random.anvil_land", pos, 0.2, 1.8);
      try { dim.spawnParticle("minecraft:critical_hit_emitter", pos); } catch {}
      remaining -= step * 0.5;            // a bounce costs a little time so it can never loop forever in one tick
      continue;
    }
    pos = { x: nx, y: ny, z: nz };
    s.traveled += step; remaining -= step;
    hitEntities(s, dim, pos);
    if (s.done) return null;
    if (s.traveled >= s.range) startReturn(s);
  }

  // orientation: thrown -> exact throw direction, afterwards slowly follows travel direction
  const ty = yawOf(s.dir), tp = pitchOf(s.dir);
  s.follow = Math.max(CFG.followBase, (s.follow ?? 0) * 0.93);
  s.yaw = lerpAngle(s.yaw, ty, s.follow);
  s.pitch = lerp(s.pitch, tp, s.follow);
  s.kick = s.kick ?? 0;                                            // bounce lean stays until the next bounce or the return
  const bank = s.kick;                                             // flat unless wallBank/floorBank are set
  s.tilt = lerp(s.tilt, bank, 0.35);
  return pos;
}

function smashPots(dim, pos) {
  for (const o of [[0, 0, 0], [0.4, 0, 0], [-0.4, 0, 0], [0, 0, 0.4], [0, 0, -0.4]]) {
    const b = blockAt(dim, pos.x + o[0], pos.y + o[1], pos.z + o[2]);
    if (b && POTS.has(b.typeId)) {
      try { dim.runCommand(`setblock ${b.x} ${b.y} ${b.z} air destroy`); } catch {}
      snd(dim, "dig.stone", pos, 1.0, 0.8);
    }
  }
}

function stepReturn(s, dim, owner) {
  let pos = s.entity.location;
  const target = sub(owner.getHeadLocation(), { x: 0, y: 0.4, z: 0 });
  const toT = sub(target, pos);
  const dist = len(toT);

  if (s.phase === "turn") {
    const t = clamp(s.phaseTick / CFG.turnTicks, 0, 1);
    const spd = Math.max(0.02, s.turnSpeed * Math.pow(1 - t, 3));         // brakes very quickly
    const blocked = isSolidBlock(blockAt(dim, pos.x + s.dir.x * (spd + RX), pos.y + s.dir.y * (spd + RY), pos.z + s.dir.z * (spd + RX)));
    if (!blocked) pos = add(pos, mul(s.dir, spd));
    hitEntities(s, dim, pos);
    // tilt and start facing the player already
    s.tilt = lerp(s.tilt, CFG.maxTilt * s.tiltSign * easeOut(t), 0.45);
    s.yaw = lerpAngle(s.yaw, yawOf(toT), 0.3);
    s.pitch = lerp(s.pitch, pitchOf(toT), 0.3);
    if (++s.phaseTick >= CFG.turnTicks) { s.phase = "back"; s.phaseTick = 0; s.dir = norm(toT); }
  } else {
    const t = clamp(s.phaseTick / CFG.accelTicks, 0, 1);
    const spd = lerp(0.12, s.cfg.speed * 1.1, easeOut(t));
    s.dir = norm(add(mul(s.dir, 0.55), mul(norm(toT), 0.45)));            // homing
    pos = add(pos, mul(s.dir, Math.min(spd, dist)));
    hitEntities(s, dim, pos);
    // realign while approaching: tilt fades to 0 and it faces the travel direction
    s.tilt = lerp(s.tilt, CFG.maxTilt * s.tiltSign * clamp((dist - 2) / 6, 0, 1), 0.25);
    s.yaw = lerpAngle(s.yaw, yawOf(s.dir), 0.4);
    s.pitch = lerp(s.pitch, pitchOf(s.dir), 0.4);
    s.phaseTick++;
  }
  smashPots(dim, pos);
  s.speed = s.phase === "back" ? lerp(0.12, s.cfg.speed, 0.5) : s.speed;
  return { pos, dist: len(sub(target, pos)) };
}

function giveBack(s, owner, dim) {
  if (s.done) return;
  s.done = true;
  discs.delete(s.id);
  let loc = s.lastPos;
  try { loc = s.entity.location; } catch {}
  killDisc(s.entity);
  const item = s.item ?? new ItemStack(s.itemId, 1);
  try { const d = item.getComponent("minecraft:durability"); if (d) d.damage = Math.min(s.dmg ?? 0, d.maxDurability); } catch {}
  if (!owner) { try { dim.spawnItem(item, loc ?? { x: 0, y: 100, z: 0 }); } catch {} return; }
  let placed = false;
  try {
    if (s.slotIndex !== undefined) {
      const sl = owner.getComponent("minecraft:inventory")?.container?.getSlot(s.slotIndex);
      if (sl && !sl.hasItem()) { sl.setItem(item); placed = true; }
    }
  } catch {}
  if (!placed) {
    let left;
    try { left = owner.getComponent("minecraft:inventory")?.container?.addItem(item); } catch {}
    if (left) { try { dim.spawnItem(left, owner.location); } catch {} }
  }
  snd(dim, "item.trident.return", owner.location, 0.7, s.cfg.snd * 1.2);
}

// ---------------------------------------------------------------- MAIN LOOP
function tick() {
  for (const [id, s] of discs) {
    try {
      if (s.done) { discs.delete(id); continue; }
      if (!isValid(s.entity)) { discs.delete(id); continue; }
      const dim = s.entity.dimension;
      const owner = getPlayer(s.ownerId);
      s.age++;
      if (s.phase === "out" && s.age > CFG.maxLifetime) startReturn(s);

      let pos, caught = false;
      if (s.phase === "out") {
        pos = stepOut(s, dim);
        if (!pos) continue;
      } else {
        if (!owner || owner.dimension.id !== dim.id) { giveBack(s, undefined, dim); continue; }
        const r = stepReturn(s, dim, owner);
        if (s.done) continue;
        pos = r.pos;
        caught = r.dist <= CFG.catchDistance;
      }
      setTilt(s);
      s.lastPos = pos;
      s.entity.teleport(pos, { rotation: { x: s.pitch, y: s.yaw } });
      if (caught) giveBack(s, owner, dim);
    } catch (e) {
      report("tick", e);
      try { giveBack(s, getPlayer(s.ownerId), s.entity.dimension); } catch { discs.delete(id); }
    }
  }
}
system.runInterval(tick, 1);

// ------------------------------------------- ORPHANS (discs left over after a reload)
function scan(giveItems) {
  for (const dimId of ["overworld", "nether", "the_end"]) {
    let list = [];
    try { list = world.getDimension(dimId).getEntities({ families: ["ore_disc"] }); } catch { continue; }
    for (const e of list) {
      if (discs.has(e.id)) continue;
      const itemId = e.getDynamicProperty("item");
      const owner = getPlayer(e.getDynamicProperty("owner"));
      if (giveItems && DISCS[itemId] && owner) {            // reload: hand the item back once
        try { owner.getComponent("minecraft:inventory").container.addItem(new ItemStack(itemId, 1)); } catch {}
      }
      killDisc(e);                                         // never leave a disc entity lying around
    }
  }
}
system.runTimeout(() => scan(true), 40);
system.runInterval(() => scan(false), 100);
