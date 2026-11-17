scoreboard players add #now od_t 1
execute as @a[scores={od_use_c=1..}] at @s run function ore_discs:throw
execute as @a[scores={od_use_w=1..}] at @s run function ore_discs:throw
scoreboard players set @a od_use_c 0
scoreboard players set @a od_use_w 0
execute as @e[type=item_display,tag=ore_disc] at @s run function ore_discs:disc/tick
