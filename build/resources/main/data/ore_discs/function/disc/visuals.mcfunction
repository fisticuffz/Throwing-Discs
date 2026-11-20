scoreboard players add @s od_spin 1
execute if score @s od_spin matches 4.. run scoreboard players set @s od_spin 0
execute if score @s od_spin matches 0 run data modify entity @s transformation.left_rotation set value [0f,0f,0f,1f]
execute if score @s od_spin matches 1 run data modify entity @s transformation.left_rotation set value [0f,0.7071f,0f,0.7071f]
execute if score @s od_spin matches 2 run data modify entity @s transformation.left_rotation set value [0f,1f,0f,0f]
execute if score @s od_spin matches 3 run data modify entity @s transformation.left_rotation set value [0f,0.7071f,0f,-0.7071f]
data modify entity @s start_interpolation set value 0
scoreboard players operation #fx od_t = @s od_dx
scoreboard players operation #fy od_t = @s od_dy
scoreboard players operation #fz od_t = @s od_dz
execute if score #fx od_t matches -9..9 run scoreboard players set #fx od_t 0
execute if score #fy od_t matches -9..9 run scoreboard players set #fy od_t 0
execute if score #fz od_t matches -9..9 run scoreboard players set #fz od_t 0
execute store result storage ore_discs:m fx double 0.0001 run scoreboard players get #fx od_t
execute store result storage ore_discs:m fy double 0.0001 run scoreboard players get #fy od_t
execute store result storage ore_discs:m fz double 0.0001 run scoreboard players get #fz od_t
function ore_discs:disc/face with storage ore_discs:m
