scoreboard players add @s od_age 1
tag @s add od_cur
function ore_discs:disc/find_owner
execute if score @s od_phase matches 0 if score @s od_age matches 1201.. run function ore_discs:disc/start_return
execute if score @s od_phase matches 0 run function ore_discs:disc/phase_out
execute if score @s od_phase matches 1 run function ore_discs:disc/phase_turn
execute if score @s od_phase matches 2 run function ore_discs:disc/phase_back
execute if score @s od_phase matches 0..2 run function ore_discs:disc/visuals
tag @s remove od_cur
tag @a[tag=od_owner] remove od_owner
