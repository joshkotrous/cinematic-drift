# Ridge Clouds: Minecraft 26.3 test recipe

Use your CurseForge 26.3 profile directory.
Restart the profile after installation. Select `CinematicDrift_DynamicClouds.zip`.
The mod controls terrain flow; the shader patch renders inside Complementary.

## Find a mountain in your actual Tectonic world

Enable cheats in a disposable test world. Use the default Overworld world type with Tectonic enabled.

```mcfunction
/gamemode spectator
/locate biome minecraft:jagged_peaks
```

The locate result returns **X and Z**. Substitute them below; Y=320 starts above most peaks:

```mcfunction
/tp @s X 320 Z
```

Fly around the ridge to load its chunks. Wait a few seconds, then:

```mcfunction
/ridgeclouds lookout
```

Click the suggested teleport command in chat and press Enter. It places you above the lee side and looks back at a nearby high ridge. It surveys loaded terrain, so revisit it after travelling or loading more chunks. If the ridge is weak, try `/locate biome minecraft:stony_peaks` or `minecraft:frozen_peaks`.

## Freeze daylight and weather

Minecraft 26.3 uses these game-rule names (verified against your local game classes):

```mcfunction
/gamerule minecraft:advance_time false
/gamerule minecraft:advance_weather false
/weather clear
/time set 1000
```

`1000` gives morning light, `6000` noon, `12000` sunset, and `18000` midnight. For comparing rain lighting use `/weather rain`, then `/weather clear`. Minecraft rain does not change Ridge Clouds' wind or density; set those explicitly.

## Start the ridge-spill test

```mcfunction
/ridgeclouds enable
/ridgeclouds preset spill
/ridgeclouds wind 90 18
/ridgeclouds fade 85
/ridgeclouds status
```

The spill preset chooses an altitude about 60 blocks below the highest **loaded** terrain. For a 250-block ridge, start with `/ridgeclouds altitude 190`. Aim for the cloud centre slightly below the crest. The wind is the direction the clouds **travel toward**: 0=east (+X), 90=south (+Z), 180=west (-X), 270=north (-Z). Select a wind that crosses the ridge, rather than following its length.

Available controls:

```mcfunction
/ridgeclouds wind 0 18
/ridgeclouds altitude 190
/ridgeclouds density 0.9
/ridgeclouds thickness 50
/ridgeclouds fade 85
/ridgeclouds quality 96
```

Wind accepts 0–360 degrees and 0–45 blocks/second. Altitude is world Y, −64 to 500. Density is 0–1.8, thickness 15–100, fade 30–200, quality 32–144 samples. A **smaller fade** value makes the cloud disappear sooner after descending; 60–85 gives a short cascade, 150–200 a longer one. Changes persist in `config/ridge-clouds.json`. Allow 5–15 seconds for the flow to settle, or run `/ridgeclouds reset` after changing the overall setup.

## Useful comparisons

Reverse wind: `/ridgeclouds wind 270 18`. The moist and dry slopes should exchange sides.
Still air: `/ridgeclouds wind 90 0`. Cloud motion should stop.
No new clouds: `/ridgeclouds disable`. Re-enable with `/ridgeclouds enable`.
Close view: fly to cloud altitude on the descending slope and watch the wisps disappear.

Use **regular Minecraft render distance of 16–24 chunks** while testing. This first build samples loaded client chunks, not Distant Horizons' stored LOD terrain. DH still renders the distant landscape, but clouds fade before leaving the sampled area. Increasing only DH's render distance does not supply the mod with more terrain.

The Ridge pack replaces Complementary's main clouds and disables vanilla/DH clouds. Original procedural cloud shadows are disabled to avoid shadows from clouds that no longer exist. The new clouds have self-shadowing; casting their own ground shadows is not implemented in this build. Complementary's sun, moon, terrain shadows, fog, water, bloom and tone mapping remain in its pipeline.

## Seed candidates found online

These are **reported seeds**, not terrain verified with your exact Tectonic 3.0.31 / Minecraft 26.3 combination. Tectonic changes the terrain, so use the biome locator above rather than assuming vanilla screenshots or coordinates match.

- `4983096615658087613`: an October 2026 vanilla report of a 1,413-block jagged/stony mountain range at spawn. [Source](https://www.reddit.com/r/minecraftseeds/comments/1x08kcq/a_mountain_range_1413_blocks_wide_featuring/).
- `100000755979678208`: an August 2026 vanilla report of a mountain range around a taiga lake at spawn. [Source](https://www.reddit.com/r/minecraftseeds/comments/1w0cakt/top_seeds_of_the_month_huge_mountain_bowls/).
- `-213424031`: a reported 3,700-block Tectonic mountain range, but the report uses **Minecraft 1.21.4**, so it is an exploratory candidate for your newer generator. [Source](https://www.reddit.com/r/minecraftseeds/comments/1leurul/insane_3700_block_long_motion_range_i_found_with/).

For an existing world, keep its seed: `/locate biome` plus `/ridgeclouds lookout` is a more reliable test route than creating a world from an unverified published seed.

To restore normal weather/daylight progression:

```mcfunction
/gamerule minecraft:advance_time true
/gamerule minecraft:advance_weather true
```

Version 0.2.0 adds /ridgeclouds scale 32. This sets the cloud-noise cell size in blocks (8–160); smaller values give finer cloud structures. Try 24, 32, and 48 from the same viewpoint. Existing wind, altitude, density and fade settings are preserved.

Version 0.3.0 supersedes the fixed density/wind test defaults: normal coverage uses the selected Complementary pack's original GetCloudNoise function, CLOUD_UNBOUND_AMOUNT and rain response. Wind follows Complementary's default +Z drift and CLOUD_SPEED_MULT. Minecraft has no native general wind field. /ridgeclouds automatic clears wind and density overrides and restores the original thin layer. /ridgeclouds density sets a relative multiplier (1 means original coverage); /ridgeclouds wind enables a simulation wind override. Spill presets no longer increase density or speed. Original shader cloud-size settings govern the native noise scale; the older scale control does not affect this native density path.

Version 0.4.0 retains only CinematicWater_RidgeClouds. Shader Options -> Atmosphere -> Clouds -> Ridge Clouds exposes Cloud Density Multiplier (108% default, 100% original), Mountain Wind Gain (1 default: up to 2x wind), and Wind Elevation Range (160 blocks above Y=64). The terrain wind boosts both advected weather and visible cloud noise through a transported motion field. Gain 0 disables the extra motion. Apply shader changes to save them; the CPU adapter reloads these settings within one second. Restart Minecraft after installing the new mod JAR.

The CinematicWater cloud menu now has Cloud Mode: Vanilla (50), Volumetric (3, the original Complementary Unbound renderer), and Dynamic (4, Ridge Clouds). Dynamic is the default. Unbound/Reimagined submenus are removed. Vanilla enables Minecraft clouds and disables the volumetric layer; Volumetric and Dynamic disable Minecraft/DH clouds to prevent duplication. Reload the shader pack or restart Minecraft after this menu update. No new mod JAR is needed.
