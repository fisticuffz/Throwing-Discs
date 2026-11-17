execute unless items entity @s weapon.mainhand *[minecraft:custom_data~{ore_disc:1b}] run return 0
scoreboard players operation #c od_t = #now od_t
scoreboard players operation #c od_t -= @s od_last
execute if score #c od_t matches ..5 run return 0
scoreboard players operation @s od_last = #now od_t
execute unless score @s od_uid matches 1.. run function ore_discs:throw_uid
execute store result storage ore_discs:m p double 0.015 run data get entity @s SelectedItem.components."minecraft:custom_data".snd
execute anchored eyes positioned ^ ^-0.3 ^0.8 run summon item_display ~ ~ ~ {Tags:["ore_disc","od_new"],item_display:"fixed",teleport_duration:2,interpolation_duration:1,transformation:{translation:[0f,0f,0f],left_rotation:[0f,0f,0f,1f],right_rotation:[-0.7071f,0f,0f,0.7071f],scale:[0.8f,0.8f,0.8f]},item:{id:"minecraft:stone",count:1}}
data modify entity @e[type=item_display,tag=od_new,limit=1] item set from entity @s SelectedItem
data modify entity @e[type=item_display,tag=od_new,limit=1] Rotation set from entity @s Rotation
scoreboard players operation @e[type=item_display,tag=od_new,limit=1] od_oid = @s od_uid
item replace entity @s weapon.mainhand with minecraft:air
execute as @e[type=item_display,tag=od_new,limit=1] at @s run function ore_discs:throw_init
function ore_discs:snd_throw with storage ore_discs:m
