execute if score #has od_t matches 0 run return run function ore_discs:disc/drop
scoreboard players set #k od_t 8
scoreboard players operation #k od_t -= @s od_tick
scoreboard players operation #c od_t = #k od_t
scoreboard players operation #c od_t *= #k od_t
scoreboard players operation #c od_t *= #k od_t
scoreboard players operation #sp od_t = @s od_tspd
scoreboard players operation #sp od_t *= #c od_t
scoreboard players operation #sp od_t /= #512 od_t
execute if score #sp od_t matches ..199 run scoreboard players set #sp od_t 200
scoreboard players operation @s od_spd = #sp od_t
function ore_discs:disc/sub
function ore_discs:disc/sub
function ore_discs:disc/sub
function ore_discs:disc/smash_near
function ore_discs:disc/hits
scoreboard players add @s od_tick 1
execute if score @s od_tick matches 8.. run function ore_discs:disc/to_back
