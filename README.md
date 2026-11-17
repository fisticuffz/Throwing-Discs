# Throwing Discs (Java, Fabric 26.3)

Right-click to throw a disc. It flies out, ricochets off walls (faster every bounce, up to 1.5x), pierces mobs, then returns to you.

- Get discs: `/function ore_discs:give/all` (or `give/copper`, `give/iron`, `give/gold`, `give/diamond`, `give/netherite`)
- Craft: 4 ingots in a plus. Netherite: netherite upgrade template + diamond disc + netherite ingot (shapeless).
- Build: `./gradlew build` -> `build/libs/`. Every push also builds on GitHub Actions (Artifacts).
- Tuning lives in the recipe results and `data/ore_discs/function/give/*.mcfunction` (`dmg`, `spd` = blocks/tick x10000, `snd` = pitch x100).

Port of a Bedrock add-on. Sharpness / Fire Aspect / Unbreaking are not ported.
