execute if score #fx od_t matches 1 run scoreboard players operation @s od_dx *= #m1 od_t
execute if score #fy od_t matches 1 run scoreboard players operation @s od_dy *= #m1 od_t
execute if score #fz od_t matches 1 run scoreboard players operation @s od_dz *= #m1 od_t
scoreboard players add @s od_rico 1
scoreboard players add @s od_range 5000
execute if score @s od_rico matches 1..4 run scoreboard players operation @s od_spd *= #11 od_t
execute if score @s od_rico matches 1..4 run scoreboard players operation @s od_spd /= #10 od_t
execute if score @s od_rico matches 5 run scoreboard players operation @s od_spd *= #10245 od_t
execute if score @s od_rico matches 5 run scoreboard players operation @s od_spd /= #10000 od_t
function ore_discs:disc/new_gen
playsound minecraft:item.trident.hit_ground player @a ~ ~ ~ 0.8 1.1
playsound minecraft:block.anvil.land player @a ~ ~ ~ 0.2 1.8
particle minecraft:crit ~ ~ ~ 0.1 0.1 0.1 0.1 6
