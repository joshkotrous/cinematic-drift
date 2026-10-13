# 0.1.1: world-load texture upload crash

The native crash report points to `Bridge.texture()` calling `glTexSubImage2D`, followed by an access violation in the NVIDIA OpenGL driver. The original upload inherited OpenGL pixel-unpack row/skip/byte-order and pixel-buffer state from the Minecraft rendering pipeline. Those settings can make a tightly packed cloud field transfer read outside its buffer or interpret the pointer as an offset into a bound pixel-unpack buffer.

`TextureUploader` now validates the direct RGBA buffer, temporarily unbinds the pixel-unpack buffer, resets all unpack settings for the tightly packed field, and restores every setting and binding in a `finally` block. The fix applies to allocation and subsequent updates. Shader packs and saved cloud settings do not need to change.

GPU regression: 100 allocation/update transfers with deliberately altered row length, image height, skip values, byte swapping, alignment, and a bound pixel-unpack buffer. Each transfer was read back from the GPU and matched exactly; every inherited state value and binding was restored. Passed on the installed RTX 4070 Ti SUPER.

The installed JAR is now `ridge-clouds-0.1.1+26.3.jar`. The previous installed JAR is backed up under `installation-backup/crash-fix-0.1.1`.

Full-mod integration check: the patched client remained alive for more than three minutes in world rendering with the CurseForge mod set (including Iris, Sodium, Distant Horizons and Tectonic). The isolated QA session was then asked to stop normally through Minecraft.stop(). Installed and tested JAR hashes match. This is a bounded smoke test, not a guarantee against every future rendering failure.
