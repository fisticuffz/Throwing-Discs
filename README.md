# Throwing Discs (Fabric, Minecraft Java 26.3)

Real Java mod: five disc items (own item ids `throwing_discs:<tier>_disc`) and a custom disc entity with ricochets,
piercing, durability, boomerang return and catch. Right-click to throw, from either hand.

Copper 5 dmg / Iron 7 / Golden 6 / Diamond 8 / Netherite 9. Craft: 4 ingots (or diamonds) in a plus shape.
Netherite: smithing table with the netherite upgrade template + a diamond disc + a netherite ingot.
`config/throwing_discs.json`: `"maxSpeedMultiplier": 0` = speed keeps growing with each ricochet, `1.5` = capped at 1.5x.

Build: `./gradlew build` -> `build/libs/throwing-discs-1.0.0.jar` (or use GitHub Actions -> Artifacts).
Based on the Bedrock add-on by MrMoonstared.
