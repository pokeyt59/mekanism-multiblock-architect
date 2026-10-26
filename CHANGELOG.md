# 0.1.0-alpha.8

First public alpha under the MekaPlan name. Minecraft 1.20.1, Forge 47.4.18.

- Unified JEMM equipment previews in EMI: live editing, performance, LDLib 3D rendering, layers and world projection.
- Optional Nova, More Thermal Evaporation and Mekanism Extras integrations.
- Nova recipe search and EMI linking, inventory shortages and saved designs.
- Required-block selection, exact-item/NBT refill, material HUD and completion notification/hiding.
- Correct placements hide; wrong blocks and supported orientation mismatches show red.
- Server-authoritative survival placement, tagged-material protection and placement-event checks.
- Cached rendering and incremental material checks.

Requires JEMM 4.27. Use TMRV **or** real JEI, never both. Addons remain optional. Assisted placement requires alpha.8 on both sides (protocol 2). Client-only preview works without MekaPlan on the server.

Verification: 53 unit tests, 13 Forge-only server placement tests and development-client integration smoke tests. Shader compatibility and every third-party configuration are not verified.
