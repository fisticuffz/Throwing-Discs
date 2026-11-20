execute at @a[tag=od_owner,limit=1] run function ore_discs:disc/spawn_item
playsound minecraft:item.trident.return player @a ~ ~ ~ 0.7 1.2
kill @s
