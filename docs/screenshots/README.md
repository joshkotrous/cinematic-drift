# In-game gallery

Captured from the running Minecraft Java 26.3 singleplayer JVM with **CinematicDrift_DynamicClouds.zip**, Ridge Clouds 0.4.0, Iris 1.11.7 and the installed terrain mods. These are Minecraft framebuffer screenshots, not generated images or lab renders.

The camera locations were selected from the loaded world heightmap around a ridge at approximately X=40, Z=136, with a peak around Y=304. The HUD was hidden during capture. The daylight pass temporarily used `/time set 6000`; time, player position, rotation, game mode and HUD settings were restored afterward.

- `daylight-crest.png`: X=190, Y=345, Z=300, yaw=137.6°, pitch=6.4°.
- `daylight-lee-slope.png`: X=80, Y=290, Z=290, yaw=164°, pitch=−5.5°.
- `twilight-valley.png`: X=−190, Y=310, Z=400, yaw=−138.9°, pitch=3.6°, original world lighting.

Coordinates refer to this existing test world, not a universally reproducible seed. Screenshots illustrate appearance; a still image does not demonstrate flow speed or performance.

## Weather and biome showcase

Six additional framebuffer captures use the same shader settings. Nearby biome candidates were identified from this world's biome source; each frame may also show neighboring biomes. Camera coordinates and commands are provided to revisit the composition in this test world:

- `desert-sunrise.png`: desert/badlands transition, `/tp @s -256 150 -256 -55 12`, `/time set 1000`, `/weather clear`.
- `forest-rain.png`: forested hills, `/tp @s 512 193 256 120 12`, `/time set 6000`, `/weather rain`.
- `plains-sunset.png`: plains and neighboring hills, `/tp @s 1280 112 -768 85 8`, `/time set 11500`, `/weather clear`.
- `snowy-slopes.png`: snowy mountain terrain, `/tp @s 1792 210 512 150 12`, `/time set 6000`, `/weather rain` (snow in cold biomes).
- `frozen-peaks.png`: frozen peaks, `/tp @s 0 240 2048 -45 10`, `/time set 6000`, `/weather clear`.
- `ridge-storm.png`: a lower viewpoint along the original ridge, `/tp @s 80 225 290 164 -5.5`, `/time set 6000`, `/weather thunder`.

These commands require permission to use cheats. Captures were made with the HUD hidden at 2694 � 1440. Original position, rotation, game mode, time, HUD, pause-on-focus behavior, and weather flags/timers were restored after the capture passes. No density or wind settings were changed for these images.
