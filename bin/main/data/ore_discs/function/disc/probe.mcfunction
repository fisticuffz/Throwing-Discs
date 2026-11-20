$execute positioned ~$(ax) ~ ~ unless block ~ ~ ~ #ore_discs:passable run scoreboard players set #fx od_t 1
$execute positioned ~ ~$(ay) ~ unless block ~ ~ ~ #ore_discs:passable run scoreboard players set #fy od_t 1
$execute positioned ~ ~ ~$(az) unless block ~ ~ ~ #ore_discs:passable run scoreboard players set #fz od_t 1
$execute positioned ~$(ax) ~ ~ if block ~ ~ ~ minecraft:decorated_pot run function ore_discs:disc/smash_here
$execute positioned ~ ~$(ay) ~ if block ~ ~ ~ minecraft:decorated_pot run function ore_discs:disc/smash_here
$execute positioned ~ ~ ~$(az) if block ~ ~ ~ minecraft:decorated_pot run function ore_discs:disc/smash_here
