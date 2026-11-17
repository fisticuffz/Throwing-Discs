scoreboard players set #has od_t 0
execute as @a[distance=0..] if score @s od_uid = @e[tag=od_cur,limit=1] od_oid run function ore_discs:disc/owner_pos
