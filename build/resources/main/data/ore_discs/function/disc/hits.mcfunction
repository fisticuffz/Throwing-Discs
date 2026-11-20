execute positioned ~-0.5 ~-0.5 ~-0.5 as @e[dx=0,dy=0,dz=0,type=!#ore_discs:ignore,tag=!od_owner] unless score @s od_hit = @e[tag=od_cur,limit=1] od_stamp run function ore_discs:disc/hit_one
execute if score @s od_max matches 1.. if score @s od_dur >= @s od_max run function ore_discs:disc/break
