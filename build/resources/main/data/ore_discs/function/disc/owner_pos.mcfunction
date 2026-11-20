scoreboard players set #has od_t 1
tag @s add od_owner
execute store result score #tx od_t run data get entity @s Pos[0] 100
execute store result score #ty od_t run data get entity @s Pos[1] 100
execute store result score #tz od_t run data get entity @s Pos[2] 100
scoreboard players add #ty od_t 122
