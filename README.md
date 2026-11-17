# Throwing Discs (Java, Fabric 26.3)
Adds Throwing discs to Minecraft Java Edition

Right-click to throw a disc. It flies out, ricochets off walls (faster every bounce, up to 1.5x), pierces mobs, then returns to you.

- Get discs: `/function ore_discs:give/all` (or `give/copper`, `give/iron`, `give/gold`, `give/diamond`, `give/netherite`)
- Craft: 4 ingots in a plus. Netherite: netherite upgrade template + diamond disc + netherite ingot (shapeless).
- Tuning lives in the recipe results and `data/ore_discs/function/give/*.mcfunction` (`dmg`, `spd` = blocks/tick x10000, `snd` = pitch x100).
