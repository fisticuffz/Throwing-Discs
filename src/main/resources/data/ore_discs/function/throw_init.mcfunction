tag @s remove od_new
scoreboard players set @s od_phase 0
scoreboard players set @s od_tick 0
scoreboard players set @s od_trav 0
scoreboard players set @s od_range 10000
scoreboard players set @s od_rico 0
scoreboard players set @s od_gen 0
scoreboard players set @s od_age 0
scoreboard players set @s od_spin 0
scoreboard players set @s od_dur 0
scoreboard players set @s od_max 0
scoreboard players add #next od_t 1
scoreboard players operation @s od_id = #next od_t
scoreboard players operation @s od_stamp = @s od_id
scoreboard players operation @s od_stamp *= #100 od_t
execute store result score @s od_base run data get entity @s item.components."minecraft:custom_data".spd
execute store result score @s od_dmgamt run data get entity @s item.components."minecraft:custom_data".dmg
execute store result score @s od_snd run data get entity @s item.components."minecraft:custom_data".snd
execute store result score @s od_max run data get entity @s item.components."minecraft:max_damage"
execute store result score @s od_dur run data get entity @s item.components."minecraft:damage"
scoreboard players operation @s od_spd = @s od_base
# direction = the unit vector the disc faces (measured by stepping 1 block forward)
execute store result score #bx od_t run data get entity @s Pos[0] 10000
execute store result score #by od_t run data get entity @s Pos[1] 10000
execute store result score #bz od_t run data get entity @s Pos[2] 10000
tp @s ^ ^ ^1
execute store result score @s od_dx run data get entity @s Pos[0] 10000
execute store result score @s od_dy run data get entity @s Pos[1] 10000
execute store result score @s od_dz run data get entity @s Pos[2] 10000
tp @s ^ ^ ^-1
scoreboard players operation @s od_dx -= #bx od_t
scoreboard players operation @s od_dy -= #by od_t
scoreboard players operation @s od_dz -= #bz od_t
