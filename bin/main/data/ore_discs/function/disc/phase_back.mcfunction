execute if score #has od_t matches 0 run return run function ore_discs:disc/drop
function ore_discs:disc/self_pos
scoreboard players operation #nx od_t = #tx od_t
scoreboard players operation #nx od_t -= #px od_t
scoreboard players operation #ny od_t = #ty od_t
scoreboard players operation #ny od_t -= #py od_t
scoreboard players operation #nz od_t = #tz od_t
scoreboard players operation #nz od_t -= #pz od_t
function ore_discs:math/norm
scoreboard players operation #len od_t = #g od_t
scoreboard players operation #wx od_t = #ux od_t
scoreboard players operation #wy od_t = #uy od_t
scoreboard players operation #wz od_t = #uz od_t
# speed ramp: 0.12 -> base*1.1 (ease-out over 12 ticks)
scoreboard players operation #k od_t = @s od_tick
execute if score #k od_t matches 12.. run scoreboard players set #k od_t 12
scoreboard players set #m od_t 12
scoreboard players operation #m od_t -= #k od_t
scoreboard players operation #m od_t *= #m od_t
scoreboard players operation #w od_t = #144 od_t
scoreboard players operation #w od_t -= #m od_t
scoreboard players operation #st od_t = @s od_base
scoreboard players operation #st od_t *= #11 od_t
scoreboard players operation #st od_t /= #10 od_t
scoreboard players remove #st od_t 1200
scoreboard players operation #st od_t *= #w od_t
scoreboard players operation #st od_t /= #144 od_t
scoreboard players add #st od_t 1200
scoreboard players operation @s od_spd = #st od_t
scoreboard players add @s od_tick 1
# caught?
scoreboard players operation #cd od_t = #st od_t
scoreboard players operation #cd od_t /= #100 od_t
scoreboard players add #cd od_t 45
execute if score #len od_t <= #cd od_t run return run function ore_discs:disc/give_back
# never overshoot the player
scoreboard players operation #d4 od_t = #len od_t
scoreboard players operation #d4 od_t *= #100 od_t
execute if score @s od_spd > #d4 od_t run scoreboard players operation @s od_spd = #d4 od_t
# homing: dir = norm(0.55*dir + 0.45*toPlayer)
scoreboard players operation #nx od_t = @s od_dx
scoreboard players operation #nx od_t *= #11 od_t
scoreboard players operation #wx od_t *= #9 od_t
scoreboard players operation #nx od_t += #wx od_t
scoreboard players operation #ny od_t = @s od_dy
scoreboard players operation #ny od_t *= #11 od_t
scoreboard players operation #wy od_t *= #9 od_t
scoreboard players operation #ny od_t += #wy od_t
scoreboard players operation #nz od_t = @s od_dz
scoreboard players operation #nz od_t *= #11 od_t
scoreboard players operation #wz od_t *= #9 od_t
scoreboard players operation #nz od_t += #wz od_t
function ore_discs:math/norm
scoreboard players operation @s od_dx = #ux od_t
scoreboard players operation @s od_dy = #uy od_t
scoreboard players operation @s od_dz = #uz od_t
scoreboard players operation #s od_t = @s od_spd
function ore_discs:disc/calc
function ore_discs:disc/move with storage ore_discs:m
function ore_discs:disc/smash_near
function ore_discs:disc/hits
