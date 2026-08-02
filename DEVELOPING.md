# Developing WynnHorizon

Technical internals for maintaining or building this mod. See
[README.md](README.md) for the player-facing description.

This is **not a fork of Voxy** and contains none of Voxy's source (Voxy is
all-rights-reserved / no-redistribution). It only injects bytecode into
Voxy's classes at runtime via Fabric Mixin, the same technique mods like
WynnVista and Voxy Auto LOD use.

## How it works

Voxy tracks LOD terrain as 512-block "top-level nodes" using a pure
ring/radius tracker (`RenderDistanceTracker`) with no public API to add or
remove individual nodes. This mod:

1. Uses two `@Accessor` mixins to reach the private `addTopLevelNode`
   callback and vertical section range on `RenderDistanceTracker`, and the
   private `renderDistanceTracker` field on `VoxyRenderSystem`.
2. Once per world session (detected via a tick-based poll, since Voxy's
   render system is created lazily), calls that callback directly for every
   node covering the configured bounding box - bypassing the ring entirely.
3. Injects into `RenderDistanceTracker#rem(x, z)` (the method the ring calls
   to unload a node) and cancels it whenever the node is inside the box, so
   the ring can never unload something force-loaded. Also cancels
   `RenderDistanceTracker#add(x, z)` for in-box nodes, since the ring
   naturally sweeps back over them as the player moves and has no idea
   they're already loaded (it was bypassed to load them in the first place)
   - without this it just spams Voxy's log with harmless but noisy
   "already in active map, discarding" errors every time the ring passes
   near the box.
4. Every tick, checks whether the player is currently inside the box and
   drives two genuinely separate Voxy knobs accordingly - driving only one
   of them leaves the other still governed by whatever Voxy's own settings
   happen to be, which is exactly what caused box/fallback boundaries to
   not be respected in an earlier version of this mod:
   - **`RenderDistanceTracker#setRenderDistance(int)`** (called directly on
     the tracker, not through `VoxyRenderSystem`'s float wrapper, which pads
     by +1 and can't express an exact zero) - controls the ring's
     load/unload radius. **Inside** the box this is set to exactly `0` so
     the ring never naturally loads anything outside the box (the box's own
     nodes don't need the ring at all, since they were force-loaded directly
     in step 2). **Outside** the box it's set to the fallback distance,
     converted from chunks to Voxy's 512-block node units and rounded up
     (Voxy has no loading granularity finer than one node).
   - **`VoxyConfig.CONFIG.sectionRenderDistance`** - a separate public field
     read directly every frame by Voxy's `HierarchicalOcclusionTraverser` as
     a squared-distance cutoff for what actually gets *drawn* (as opposed to
     what's loaded). **Inside** the box this is sized to cover the farthest
     box corner from the player's current position, so the whole box is
     visible regardless of where in it the player stands. **Outside** the
     box it's set to the exact fallback distance (unrounded, since drawing
     has no node-granularity restriction), so nothing beyond that small
     radius draws even if it happens to still be resident from the ring.

If Voxy isn't installed, a custom Mixin config plugin (`VoxyMixinPlugin`)
skips applying any of the above entirely, and the mod just logs a warning
and stays idle - it won't crash your game or modpack.

## Mod Menu integration

`ModMenuIntegration` is registered under the `"modmenu"` entrypoint key in
`fabric.mod.json`. It's only ever touched by Mod Menu's own code looking up
that entrypoint, so it's never classloaded - and its direct Cloth
Config/Mod Menu references never resolved - unless Mod Menu is actually
installed. Cloth Config isn't separately guarded, since Mod Menu itself
generally requires it to render a screen at all.

The generated screen has two categories: **General** (enabled, outside
render distance, HUD toggle - all applied immediately on save, since
`VoxyBridge` reads `BoundsConfig.INSTANCE` fresh every tick) and
**Advanced** (the box bounds), which warns via tooltip that shrinking the
box doesn't retroactively unload already-loaded terrain. The screen's
`setSavingRunnable` calls `cfg.save()` and, if Voxy is loaded,
`VoxyBridge.forceReload()` - which re-runs the force-load pass and
re-applies distances immediately, so growing the box (or changing the
render distance) takes effect the moment you save, without needing to
rejoin.

## Building

Requires a Voxy jar at compile time. Rather than a jar in `libs/`, this
project pulls it straight from Modrinth's Maven-compatible endpoint (the
same trick Voxy's own `build.gradle` uses for its Sodium dependency, and
the same endpoint Mod Menu and Cloth Config are pulled from):

```groovy
repositories {
    maven { url = "https://api.modrinth.com/maven" }
}
dependencies {
    modImplementation "maven.modrinth:voxy:${voxy_version}"
}
```

`voxy_version` lives in [gradle.properties](gradle.properties). It's
**compile/dev-runtime only** - it is never bundled or shaded into the built
jar (no `include(...)`), so the distributed mod stays just this project's
own bytecode, in line with Voxy's license. Mod Menu and Cloth Config are
likewise never bundled - both are soft/optional dependencies, only listed
under `recommends` in `fabric.mod.json`.

```
./gradlew build
```

The output jar is in `build/libs/`. Requires both this mod **and** Voxy
installed to do anything; install both jars plus Fabric API in your mods
folder to test. Add Mod Menu + Cloth Config too if you're testing the
settings screen.

### Versions this is built/tested against

Pinned in `gradle.properties`, currently:

- Minecraft `1.21.11` - this is pinned to whatever Wynncraft's server
  actually runs, **not** Voxy's latest supported client version (Voxy
  itself has since moved on to newer Minecraft releases on its default
  branch; this project intentionally tracks behind it).
- Fabric Loader `0.19.3`, Fabric API `0.141.6+1.21.11`
- Voxy `0.2.16-beta` (the last version Voxy published for `1.21.11`)
- Mod Menu `17.0.0`, Cloth Config `21.11.153+fabric` (both normal,
  well-formed Fabric mods for this MC version - no access-widener quirks
  like Voxy below, so both use plain `modImplementation`).
- Official Mojang mappings via `loom.officialMojangMappings()` - Voxy itself
  is written against these, not Yarn, and 1.21.11 ships obfuscated so this
  is required (unlike some later Minecraft releases that ship unobfuscated,
  where this call and the `mod*` dependency configurations stop applying
  entirely - if you ever bump `minecraft_version` past that point, expect
  build errors here that point you at the fix).
- Java 21 (matches the bytecode version Voxy 0.2.16-beta itself is
  compiled for; verified directly against the published jar).

**If you bump `voxy_version` (or `minecraft_version`),** re-check:

- `me.cortex.voxy.client.core.rendering.RenderDistanceTracker` for the
  private field names the accessor mixin targets
  (`addTopLevelNode`/`removeTopLevelNode`/`minSec`/`maxSec`) and the
  `rem(int, int)`/`add(int, int)` method signatures the cancelling mixin
  targets.
- `me.cortex.voxy.client.core.VoxyRenderSystem` for the private
  `renderDistanceTracker` field.
- `me.cortex.voxy.client.core.IGetVoxyRenderSystem` - the interface
  `VoxyBridge` casts `Minecraft.getInstance().levelRenderer` to, to reach
  the live render system. **This one has already changed once**: Voxy's
  newest (post-`1.21.11`) releases renamed it to `IVoxyRenderSystemHolder`
  with an extra `voxy$setWorld` method. Don't assume the name in this repo
  is current for whatever version you're targeting - check the actual jar
  (e.g. `javap -p` on the extracted `.class` file is enough, no decompiler
  needed, since a mod's own class/member names are never obfuscated
  regardless of Minecraft version).
- `me.cortex.voxy.client.config.VoxyConfig` for the public
  `sectionRenderDistance` field `VoxyBridge` writes to directly.

If any of these are wrong for the version you build against, the game will
fail to start with a Mixin apply error rather than silently misbehaving
(`required: true` in the mixin config), which is at least a clear signal
something here needs updating.

## Config file

`config/wynnhorizon.json` (created on first run):

```json
{
  "enabled": true,
  "minX": -2512,
  "maxX": 1553,
  "minZ": -5774,
  "maxZ": -207,
  "fallbackRenderDistance": 20.0,
  "showHud": false
}
```

The default box is Wynncraft's main-map bounding box, sourced from
WynnVista's shipped `WynnVistaMod.checkAndUpdateRenderDistance`. Treat it as
a good starting point, not gospel - Wynncraft's playable area can shift.

`fallbackRenderDistance` is in **Minecraft chunks** (16 blocks), matching
vanilla's own render-distance slider - not Voxy's 512-block node units. It
only applies outside the box; inside the box only renders the box itself.
Voxy's LOD loading can't go finer than one 512-block node (32 chunks), so
small values (e.g. the default 20) load a slightly larger area than
requested outside the box - but what's actually *drawn* still respects the
exact configured distance, since the draw cutoff isn't bound by that same
rounding (see `BoundsConfig#fallbackRingSections`/`fallbackCullSections`).

## Testing in a dev environment

1. `./gradlew runClient` - Voxy, Mod Menu, and Cloth Config are all
   `modImplementation`/`compileOnly` dependencies (see above), so Loom
   already puts Mod Menu and Cloth Config in the dev run's mods
   automatically; Voxy needs the extra step noted in `build.gradle`'s
   comment on its `compileOnly` line, since it's deliberately excluded from
   normal mod-dependency processing.
2. Join/create a world and confirm the log shows
   `Force-loaded <N> Voxy top-level nodes covering bounding box ...`.
3. Enable the debug HUD (via Mod Menu, or hand-edit `showHud` in the config
   and rejoin) and fly toward one edge of the box, then past it: the HUD
   should flip from "Inside" to "Outside" right at the configured
   coordinate.
4. While standing well inside the box, look toward the nearest edge: you
   should see box terrain and *nothing* beyond the edge - no event
   islands, void terrain, or anything else outside the box, even right up
   against the boundary. Fly toward the far side of the box: it should
   already be fully visible (the whole box renders from anywhere inside it),
   confirming the proactive force-load isn't waiting for proximity.
5. Cross to the outside: box terrain should mostly disappear except for
   whatever's within `fallbackRenderDistance` chunks of your current
   position, and it should shrink/reappear as you move further from or back
   toward the edge - same as normal terrain, not specially forced.
6. Fly far outside the box into open space: only a small ring around you
   should render (the fallback radius, in chunks), not the whole map.
7. Open the Mod Menu screen, edit a box bound in the Advanced category, and
   save: the log should show a fresh `Force-loaded ...` line immediately
   (confirming `setSavingRunnable` triggers `VoxyBridge.forceReload()`
   without needing a rejoin).

## Known limitations

- **Pinned to one Voxy version's internals.** This relies on private field
  names inside `RenderDistanceTracker`/`VoxyRenderSystem` that Voxy could
  rename or restructure in a future release; see "Versions this is built
  against" above for what to re-check when updating.
- **Shrinking the box at runtime doesn't retroactively unload nodes.**
  Nodes force-loaded under a previous, larger box aren't proactively purged
  when you shrink it - they'll only get reclaimed if the player's radius
  ring naturally passes over them later, or on rejoin. Surfaced to players
  via a tooltip warning in the Mod Menu screen's Advanced category.
- **This mod's fallback distance overrides Voxy's own render-distance
  slider** every tick while enabled (by design - it's what makes the
  "small radius outside the box" behavior stick, and what keeps the box
  itself from being distance-culled), so adjusting Voxy's native render
  distance slider in its own config screen while this mod is enabled will
  have no visible effect. This overrides both `RenderDistanceTracker`'s ring
  radius (load/unload) and `VoxyConfig.CONFIG.sectionRenderDistance` (actual
  draw cutoff) - if you open Voxy's own settings screen and hit its
  save/apply button while this mod is enabled, it will likely persist
  whatever inflated value this mod was driving at that moment into Voxy's
  own config file; avoid doing that, or just let this mod re-drive the
  value back on the next tick and ignore what Voxy's screen shows.
- **No in-world rendered box outline.** `fabric-rendering-v1` does expose a
  `WorldRenderEvents`-style API on `1.21.11` (unlike some later Minecraft
  releases where it's been replaced), so this is a feasible future addition,
  not a version limitation - it just wasn't implemented in this pass.
