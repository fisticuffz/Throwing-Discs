scoreboard players add @s od_gen 1
execute if score @s od_gen matches 100.. run scoreboard players set @s od_gen 1
scoreboard players operation @s od_stamp = @s od_id
scoreboard players operation @s od_stamp *= #100 od_t
scoreboard players operation @s od_stamp += @s od_gen
