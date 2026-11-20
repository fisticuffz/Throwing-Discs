scoreboard players operation #sx od_t = @s od_dx
scoreboard players operation #sx od_t *= #s od_t
scoreboard players operation #sx od_t /= #10000 od_t
scoreboard players operation #sy od_t = @s od_dy
scoreboard players operation #sy od_t *= #s od_t
scoreboard players operation #sy od_t /= #10000 od_t
scoreboard players operation #sz od_t = @s od_dz
scoreboard players operation #sz od_t *= #s od_t
scoreboard players operation #sz od_t /= #10000 od_t
execute if score #sx od_t matches -9..9 run scoreboard players set #sx od_t 0
execute if score #sy od_t matches -9..9 run scoreboard players set #sy od_t 0
execute if score #sz od_t matches -9..9 run scoreboard players set #sz od_t 0
scoreboard players operation #ax od_t = #sx od_t
scoreboard players operation #ay od_t = #sy od_t
scoreboard players operation #az od_t = #sz od_t
execute if score @s od_dx matches 50.. run scoreboard players add #ax od_t 3000
execute if score @s od_dx matches ..-50 run scoreboard players remove #ax od_t 3000
execute if score @s od_dy matches 50.. run scoreboard players add #ay od_t 2000
execute if score @s od_dy matches ..-50 run scoreboard players remove #ay od_t 2000
execute if score @s od_dz matches 50.. run scoreboard players add #az od_t 3000
execute if score @s od_dz matches ..-50 run scoreboard players remove #az od_t 3000
execute store result storage ore_discs:m sx double 0.0001 run scoreboard players get #sx od_t
execute store result storage ore_discs:m sy double 0.0001 run scoreboard players get #sy od_t
execute store result storage ore_discs:m sz double 0.0001 run scoreboard players get #sz od_t
execute store result storage ore_discs:m ax double 0.0001 run scoreboard players get #ax od_t
execute store result storage ore_discs:m ay double 0.0001 run scoreboard players get #ay od_t
execute store result storage ore_discs:m az double 0.0001 run scoreboard players get #az od_t
