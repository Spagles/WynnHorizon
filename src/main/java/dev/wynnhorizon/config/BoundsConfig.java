package dev.wynnhorizon.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.wynnhorizon.WynnHorizonMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Plain JSON config, editable by hand, via the Mod Menu screen
 * ({@code ModMenuIntegration}), or the two {@code /wynnhorizon} chat
 * commands. Default box is Wynncraft's main-map bounding box as shipped in
 * WynnVista's {@code checkAndUpdateRenderDistance} - treat it as a good
 * starting point, not gospel, since the true playable area can shift over
 * time.
 */
public final class BoundsConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("wynnhorizon.json");

    public static BoundsConfig INSTANCE = load();

    public boolean enabled = true;

    public int minX = -2512;
    public int maxX = 1553;
    public int minZ = -5774;
    public int maxZ = -207;

    /**
     * Render distance used outside the box, in Minecraft chunks (16 blocks) -
     * matches the unit players already know from vanilla's render-distance
     * slider. Not used at all while the player is inside the box: inside,
     * only the box itself renders (see {@link #fallbackRingSections()} /
     * {@link #fallbackCullSections()} for how this gets converted into
     * Voxy's own 512-block "top-level node" units for the two different
     * things it controls).
     */
    public float fallbackRenderDistance = 20;

    /** Off by default - it's a debugging aid, not something most players need on by default. */
    public boolean showHud = false;

    public static BoundsConfig load() {
        if (Files.exists(PATH)) {
            try {
                String json = Files.readString(PATH);
                BoundsConfig loaded = GSON.fromJson(json, BoundsConfig.class);
                if (loaded != null) {
                    return loaded;
                }
            } catch (IOException | RuntimeException e) {
                WynnHorizonMod.LOGGER.error("Failed to read wynnhorizon.json, falling back to defaults", e);
            }
        }
        BoundsConfig fresh = new BoundsConfig();
        fresh.save();
        return fresh;
    }

    public void save() {
        try {
            Files.createDirectories(PATH.getParent());
            Files.writeString(PATH, GSON.toJson(this));
        } catch (IOException e) {
            WynnHorizonMod.LOGGER.error("Failed to write wynnhorizon.json", e);
        }
    }

    public boolean isBlockInsideBox(double x, double z) {
        int blockX = (int) Math.floor(x);
        int blockZ = (int) Math.floor(z);
        return blockX >= this.minX && blockX <= this.maxX && blockZ >= this.minZ && blockZ <= this.maxZ;
    }

    /** Voxy top-level nodes are 512 blocks (2^9) wide; block coords convert with a plain {@code >> 9}. */
    public int nodeMinX() {
        return this.minX >> 9;
    }

    public int nodeMaxX() {
        return this.maxX >> 9;
    }

    public int nodeMinZ() {
        return this.minZ >> 9;
    }

    public int nodeMaxZ() {
        return this.maxZ >> 9;
    }

    public boolean isNodeInsideBox(int nodeX, int nodeZ) {
        return nodeX >= nodeMinX() && nodeX <= nodeMaxX() && nodeZ >= nodeMinZ() && nodeZ <= nodeMaxZ();
    }

    /**
     * {@code RenderDistanceTracker}'s ring only operates in whole 512-block
     * node units, so a chunk-scale fallback (e.g. 20 chunks = 320 blocks)
     * has to round UP to the nearest node - Voxy's LOD system simply has no
     * finer loading granularity than one top-level node. The actual visible
     * cutoff is still correct at the exact requested distance because
     * {@link #fallbackCullSections()} (the draw-distance cutoff, not the
     * load radius) isn't rounded - this just means slightly more than the
     * requested area sits loaded in memory outside the box, not that
     * anything extra is drawn.
     */
    public int fallbackRingSections() {
        return Math.max(1, (int) Math.ceil((this.fallbackRenderDistance * 16.0) / 512.0));
    }

    /** Exact (unrounded) node-unit distance for the GPU draw-distance cutoff - see {@link #fallbackRingSections()}. */
    public float fallbackCullSections() {
        return (float) ((this.fallbackRenderDistance * 16.0) / 512.0);
    }
}
