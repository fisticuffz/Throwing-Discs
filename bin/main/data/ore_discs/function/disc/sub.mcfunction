execute unless score @s od_phase matches 0..1 run return 0
scoreboard players set #fx od_t 0
scoreboard players set #fy od_t 0
scoreboard players set #fz od_t 0
scoreboard players set #pot od_t 0
scoreboard players operation #s od_t = @s od_spd
scoreboard players operation #s od_t /= #3 od_t
function ore_discs:disc/calc
function ore_discs:disc/probe with storage ore_discs:m
execute if score #fx od_t matches 0 if score #fy od_t matches 0 if score #fz od_t matches 0 run function ore_discs:disc/probe_corner with storage ore_discs:m
execute if score #pot od_t matches 1 if score @s od_phase matches 0 run return run function ore_discs:disc/start_return
scoreboard players operation #blk od_t = #fx od_t
scoreboard players operation #blk od_t += #fy od_t
scoreboard players operation #blk od_t += #fz od_t
execute if score #blk od_t matches 1.. if score @s od_phase matches 0 run return run function ore_discs:disc/ricochet
execute if score #blk od_t matches 0 run function ore_discs:disc/move with storage ore_discs:m
scoreboard players operation #t10 od_t = #s od_t
scoreboard players operation #t10 od_t /= #10 od_t
execute if score @s od_phase matches 0 run scoreboard players operation @s od_trav += #t10 od_t
