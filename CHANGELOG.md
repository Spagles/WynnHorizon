# Changelog

## 1.0.0

Initial public release.

- Force-loads Wynncraft's main map as a fixed bounding box in Voxy, so it's
  always fully rendered regardless of player distance - no more terrain
  popping in and out.
- While inside the main map, only the main map renders; nothing outside it
  loads or draws, even right at the edge.
- While outside the main map (event areas, the void, etc.), falls back to a
  small, configurable render distance around the player instead.
- In-game settings screen via Mod Menu + Cloth Config (optional), covering
  every config field, including the map bounds themselves under an
  "Advanced" category with a warning about the rejoin-to-fully-apply
  behavior when shrinking the box.
- Two chat commands for quick access without opening a menu:
  `/wynnhorizon toggle` and `/wynnhorizon setoutsiderender <chunks>`.
