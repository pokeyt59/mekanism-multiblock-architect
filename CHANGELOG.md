# 0.1.0-alpha.9-jei.1 (unofficial JEI port)

- Host the planner in JEI instead of EMI. One JEI category collects the JEMM, Mekanism Extras, More Thermal Evaporation and Nova designs and hides the pages they came from.
- EMI and TooManyRecipeViewers are no longer needed; JEI 15.38 or newer is required.
- Nova's recipe-to-machine link is a button beside JEI recipes.
- Planner search and favorites boxes receive typing before JEI's shortcuts.
- Planner pages free their preview GPU buffers once JEI stops drawing them.

Based on 0.1.0-alpha.9. Placement protocol 2 is unchanged.

# 0.1.0-alpha.9

- Remove the five-item limit from the construction material HUD.
- Grow the list to the screen height; Alt + mouse wheel reaches every overflow row.
- Wrap long item names, align counts and avoid covering the target-block card.
- Keep normal hotbar scrolling, material quantities and placement protocol unchanged.

Client HUD fix; placement protocol 2 remains compatible with alpha.8 servers.

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
