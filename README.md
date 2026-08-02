# WynnHorizon

No more terrain popping in and out as you fly around Wynncraft's main map.
WynnHorizon keeps the whole map permanently loaded and visible in
[Voxy](https://modrinth.com/mod/voxy) (LOD terrain mod), while everywhere
else - event islands, the void, wherever - stays on a small, normal render
distance around you.

## What's Up

- The Wynncraft main map is always fully rendered, no matter how far away
  you are on it - it's force-loaded once and never pops in or out.
- Everywhere outside the main map uses a small, configurable render distance
  around you instead, so temporary event areas and the void don't linger in
  view once you leave them.
- Configurable in-game via [Mod Menu](https://modrinth.com/mod/modmenu), or
  two quick chat commands.

## Installation

1. Install Fabric Loader and Fabric API.
2. Install [Voxy](https://modrinth.com/mod/voxy) 0.2.16-beta or newer.
3. *(Optional)* Install [Mod Menu](https://modrinth.com/mod/modmenu) and
   [Cloth Config](https://modrinth.com/mod/cloth-config) for the in-game
   settings screen.
4. Drop the jar in your mods folder.

WynnHorizon does nothing on its own - it needs Voxy installed to have
anything to hook into.

## Configuration

Open Mod Menu and find WynnHorizon for the full settings screen:

- **Enabled** - turns the whole mod on or off.
- **Outside Render Distance** - render distance, in chunks, used outside the
  main map.
- **Show Debug HUD** - a small on-screen readout of what the mod's doing
  right now.
- **Advanced** - the main map's bounding box itself. Most players never need
  to touch this; it already defaults to Wynncraft's real map. Shrinking it
  doesn't unload already-loaded terrain until you rejoin the world.

Or from chat, without opening a menu:

- `/wynnhorizon toggle` - enable/disable the mod.
- `/wynnhorizon setoutsiderender <chunks>` - set the outside-map render
  distance.

No Mod Menu? Everything above also lives in `config/wynnhorizon.json`,
hand-editable and picked up next launch.

## Requirements

- Minecraft `1.21.11`
- Fabric Loader `0.19.3+`
- Fabric API
- Voxy `0.2.16-beta+`
- Mod Menu + Cloth Config *(optional, only for the in-game settings screen)*

## Notes

- Adjusting Voxy's own render-distance slider has no visible effect while
  this mod is enabled outside the map - use the setting above instead.
- Maintaining or building this yourself? See [DEVELOPING.md](DEVELOPING.md)
  for how it actually hooks into Voxy and what to check if you bump the
  Voxy version.

## Credits

- [Cortex](https://modrinth.com/mod/voxy) for Voxy.
- [DrBiznes](https://github.com/DrBiznes/WynnVista) for WynnVista, whose
  Voxy integration this project's approach is based on.

Found a bug or have a feature idea? Open an issue.
