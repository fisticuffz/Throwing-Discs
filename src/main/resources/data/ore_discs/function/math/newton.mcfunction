scoreboard players operation #q od_t = #l2 od_t
scoreboard players operation #q od_t /= #g od_t
scoreboard players operation #g od_t += #q od_t
scoreboard players operation #g od_t /= #2 od_t
execute if score #g od_t matches ..0 run scoreboard players set #g od_t 1
