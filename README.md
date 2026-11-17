# Throwing Discs - Fabric mod (Minecraft Java 26.2)

A real Fabric mod: a custom disc entity with its own physics (real ricochets off block faces, piercing, durability,
boomerang return, catch right before touching you), five discs, recipes, enchanting, sounds.

I (Claude) could not compile this myself - no network or Minecraft libraries in my sandbox - so it is written from the
official Fabric docs for 26.2 but UNTESTED. Compile errors are possible; they are quick to fix in an IDE.
If you hit one, paste the error to me and I will fix it.

## What you need
* JDK **25** (Minecraft 26.x requires Java 25)
* Internet (Gradle downloads Minecraft, Loom and Fabric API on first build)
* Fabric Loader 0.19.3+ and Fabric API in your game when you play it

## Way 1 - build on GitHub (no local setup)
1. Create a new GitHub repository and upload everything in this folder (keep the `.github` folder!).
2. Open the **Actions** tab -> "build" -> wait for the green check.
3. Download the `throwing-discs-jar` artifact: it contains `throwing-discs-1.0.0.jar` (the `-sources.jar` is not needed).

## Way 2 - build locally (recommended if you have JDK 25)
The Gradle wrapper (`gradlew`) cannot be generated without network access, so take it from the official template:
1. Go to https://fabricmc.net/develop/template , choose **26.2**, untick data generation/split sources if you like, download.
2. Unzip it. Copy these files from the template into this folder (overwrite): `gradlew`, `gradlew.bat`, the `gradle/` folder.
   (Optionally also copy its `build.gradle`/`gradle.properties` - they should match ours - 3 version numbers).
3. In this folder run `./gradlew build` (Windows: `gradlew.bat build`). The jar is in `build/libs/throwing-discs-1.0.0.jar`.
4. To test in a dev client: `./gradlew runClient`. In VS Code: install "Extension Pack for Java" + "Gradle for Java" and open the folder.

## Other Minecraft versions
Open https://fabricmc.net/develop , pick your version and copy `minecraft_version`, `loader_version`, `loom_version` and
`fabric_api_version` into `gradle.properties`. Also change `"minecraft": "~26.2"` in `src/main/resources/fabric.mod.json`.
Mojang's names/APIs change between versions, so other versions may need small code fixes.

## Using the mod
Creative inventory (Combat tab) or `/give @s throwing_discs:iron_disc`. Right-click to throw, from either hand.
Copper 5 dmg / Iron 7 / Golden 6 / Diamond 8 / Netherite 9. Craft: 4 ingots (or diamonds) in a plus shape.
Netherite: smithing table with the netherite upgrade template, a diamond disc and a netherite ingot.
Config: `config/throwing_discs.json` -> `"maxSpeedMultiplier": 0` = speed keeps increasing with every ricochet,
`1.5` = capped at 1.5x the starting speed.

## Flat spinning renderer (optional second step)
The mod first uses vanilla's `ThrownItemRenderer` (a sprite that faces the camera) because that API is the same on
every version and is certain to compile. To get the flat, spinning, pitched disc:
1. Move `optional/ThrowingDiscFlatRenderer.java.txt` to `src/client/java/com/throwingdiscs/client/ThrowingDiscFlatRenderer.java`.
2. In `ThrowingDiscsClient.java` replace the register line with
   `EntityRenderers.register(ModEntities.DISC, ThrowingDiscFlatRenderer::new);`
3. Fix any red imports with your IDE's quick-fix (the rendering API is the most change-prone part of Minecraft).

`reference/bedrock_main.js` is the finished Bedrock version of the same behaviour, for comparison.
