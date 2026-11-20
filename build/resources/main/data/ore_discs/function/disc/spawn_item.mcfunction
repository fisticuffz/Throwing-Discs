summon item ~ ~ ~ {Item:{id:"minecraft:stone",count:1},PickupDelay:0,Tags:["od_drop"]}
data modify entity @e[type=item,tag=od_drop,limit=1] Item set from entity @e[tag=od_cur,limit=1] item
execute store result storage ore_discs:m dmg int 1 run scoreboard players get @e[tag=od_cur,limit=1] od_dur
function ore_discs:disc/set_dmg with storage ore_discs:m
tag @e[type=item,tag=od_drop] remove od_drop
