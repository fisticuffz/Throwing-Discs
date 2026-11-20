execute if score #nx od_t matches 20001.. run scoreboard players set #nx od_t 20000
execute if score #nx od_t matches ..-20001 run scoreboard players set #nx od_t -20000
execute if score #ny od_t matches 20001.. run scoreboard players set #ny od_t 20000
execute if score #ny od_t matches ..-20001 run scoreboard players set #ny od_t -20000
execute if score #nz od_t matches 20001.. run scoreboard players set #nz od_t 20000
execute if score #nz od_t matches ..-20001 run scoreboard players set #nz od_t -20000
scoreboard players operation #l2 od_t = #nx od_t
scoreboard players operation #l2 od_t *= #nx od_t
scoreboard players operation #q od_t = #ny od_t
scoreboard players operation #q od_t *= #ny od_t
scoreboard players operation #l2 od_t += #q od_t
scoreboard players operation #q od_t = #nz od_t
scoreboard players operation #q od_t *= #nz od_t
scoreboard players operation #l2 od_t += #q od_t
scoreboard players operation #g od_t = #nx od_t
execute if score #g od_t matches ..-1 run scoreboard players operation #g od_t *= #m1 od_t
scoreboard players operation #a od_t = #ny od_t
execute if score #a od_t matches ..-1 run scoreboard players operation #a od_t *= #m1 od_t
execute if score #a od_t > #g od_t run scoreboard players operation #g od_t = #a od_t
scoreboard players operation #a od_t = #nz od_t
execute if score #a od_t matches ..-1 run scoreboard players operation #a od_t *= #m1 od_t
execute if score #a od_t > #g od_t run scoreboard players operation #g od_t = #a od_t
scoreboard players set #ux od_t 0
scoreboard players set #uy od_t 0
scoreboard players set #uz od_t 0
execute if score #g od_t matches 0 run return 0
function ore_discs:math/newton
function ore_discs:math/newton
function ore_discs:math/newton
function ore_discs:math/newton
function ore_discs:math/newton
scoreboard players operation #ux od_t = #nx od_t
scoreboard players operation #ux od_t *= #10000 od_t
scoreboard players operation #ux od_t /= #g od_t
scoreboard players operation #uy od_t = #ny od_t
scoreboard players operation #uy od_t *= #10000 od_t
scoreboard players operation #uy od_t /= #g od_t
scoreboard players operation #uz od_t = #nz od_t
scoreboard players operation #uz od_t *= #10000 od_t
scoreboard players operation #uz od_t /= #g od_t
