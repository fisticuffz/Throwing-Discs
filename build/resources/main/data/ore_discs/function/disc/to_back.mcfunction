scoreboard players set @s od_phase 2
scoreboard players set @s od_tick 0
function ore_discs:disc/self_pos
scoreboard players operation #nx od_t = #tx od_t
scoreboard players operation #nx od_t -= #px od_t
scoreboard players operation #ny od_t = #ty od_t
scoreboard players operation #ny od_t -= #py od_t
scoreboard players operation #nz od_t = #tz od_t
scoreboard players operation #nz od_t -= #pz od_t
function ore_discs:math/norm
scoreboard players operation @s od_dx = #ux od_t
scoreboard players operation @s od_dy = #uy od_t
scoreboard players operation @s od_dz = #uz od_t
