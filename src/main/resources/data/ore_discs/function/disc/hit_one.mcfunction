scoreboard players operation @s od_hit = @e[tag=od_cur,limit=1] od_stamp
execute store result storage ore_discs:m amt int 1 run scoreboard players get @e[tag=od_cur,limit=1] od_dmgamt
execute if score #has od_t matches 1 run function ore_discs:disc/dmg_by with storage ore_discs:m
execute if score #has od_t matches 0 run function ore_discs:disc/dmg_plain with storage ore_discs:m
playsound minecraft:item.trident.hit player @a ~ ~ ~ 0.8 1.2
scoreboard players add @e[tag=od_cur,limit=1] od_dur 1
