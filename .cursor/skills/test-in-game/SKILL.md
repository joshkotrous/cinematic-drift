---
name: test-in-game
description: Tests Cinematic Drift terrain-responsive clouds in Minecraft and captures weather and biome showcase screenshots. Use when asked to validate the mod in game or capture its appearance.
disable-model-invocation: true
---

# Test Cinematic Drift in Minecraft

Read the repository README and mod/src/main/resources/fabric.mod.json for current versions. Identify the actual running profile, JVM and selected shader; do not assume a path or PID. Prefer a disposable singleplayer world with Tectonic.

## Prepare and preserve state

1. Build with `python mod/build-ci.py` using JDK 25 and run `npm test`. Follow the repository installation instructions. Minecraft must be closed before replacing its loaded mod JAR.
2. Select `CinematicDrift_DynamicClouds.zip` and Cloud Mode **Dynamic**. Vanilla and Volumetric are comparison modes; use the pack's mode handling to prevent duplicate vanilla/Distant Horizons clouds.
3. Before changing an existing world, record position, rotation, mode, time, weather flags/timers, progression rules, HUD and pause-on-lost-focus. Restore every changed setting in a finally block, including on failure. Preserve the user's density/wind preferences.
4. For normal defaults use `/ridgeclouds automatic`. Coverage follows Complementary's original noise and rain response; drift follows its +Z direction and CLOUD_SPEED_MULT. Minecraft has no general vanilla wind field. Explicit wind/density commands enable overrides.

## Find a ridge

With cheat permission:

```mcfunction
/gamemode spectator
/locate biome minecraft:jagged_peaks
/tp @s X 320 Z
/ridgeclouds lookout
```

Substitute the locator's X/Z. Load nearby chunks before running lookout, then use its suggested teleport in chat. Try stony_peaks or frozen_peaks for another mountain. Published vanilla seeds do not guarantee the same Tectonic terrain.

Use regular render distance 16–24 chunks where practical. The mod samples loaded client chunks; Distant Horizons LOD terrain does not supply its heightmap. Wait 15–30 seconds after travel or environment changes before judging the result.

## Validate appearance and motion

- Watch lift on the windward slope, crest crossing, descent on the lee slope and fading rather than a long downstream ribbon.
- In Shader Options / Atmosphere / Clouds / Dynamic Clouds, compare density 100% with default 108%, Mountain Wind Gain 0 with 1, and Wind Elevation Range (default 160 blocks above Y=64). Gain 1 allows up to 2x drift at elevation. Apply changes and restore original settings afterward.
- Inspect soft edges, volumetric lighting and terrain depth from above and within clouds. Check that water, fog, bloom, sun/moon and terrain lighting remain intact.
- Compare Vanilla, Volumetric and Dynamic from the same camera/weather; inspect logs for shader errors and exceptions.
- Use explicit wind reversal or zero-wind overrides only for controlled motion checks, then restore automatic/user settings.
- Record actual FPS/frame times and test conditions for performance claims. Screenshots do not demonstrate flow speed or performance.

## Showcase weather and biomes

```mcfunction
/time set 1000
/weather clear
/time set 6000
/weather rain
/time set 11500
/weather thunder
```

Capture sunrise, noon and sunset across desert, forest, plains and snowy mountains. Rain becomes snow in cold biomes. Refer to `docs/screenshots/README.md` at the repository root for existing viewpoints. Keep nighttime photographs out of the main README unless requested.

If freezing progression, 26.3 uses `/gamerule minecraft:advance_time false` and `/gamerule minecraft:advance_weather false`. Restore their recorded values rather than blindly setting true.

Use F2 or Minecraft's framebuffer API with HUD hidden. Wait for asynchronous PNG writes to finish, inspect every image visually, and store selected images under `docs/screenshots/`. Include accurate weather/time/camera metadata; a frame may contain neighboring biomes.

## JVM attachment, when authorized by the user

Use Java's Attach API with a uniquely named agent class for each revision. Inspect local game classes before assuming older APIs. In 26.3, `Minecraft.execute(Runnable)` schedules client/render work, `setScreenAndShow(null)` closes menus, `gui.hud.isHidden()/toggle()` controls the HUD, `level.getOverworldClockTime()` reads time and `Screenshot.grab(minecraft, false)` saves a framebuffer. Schedule server terrain/weather work on the server thread. WeatherData contains clear/rain/thunder timers and flags. Restore state in finally and verify completion.

## Deliver

Report findings, actual checks performed, measured performance if available and remaining limitations. Link the gallery or resulting commit. A successful build or shader compile alone is not successful in-game testing.
