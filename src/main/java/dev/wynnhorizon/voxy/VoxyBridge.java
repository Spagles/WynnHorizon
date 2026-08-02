package dev.wynnhorizon.voxy;

import dev.wynnhorizon.WynnHorizonMod;
import dev.wynnhorizon.config.BoundsConfig;
import dev.wynnhorizon.mixin.RenderDistanceTrackerAccessor;
import dev.wynnhorizon.mixin.VoxyRenderSystemAccessor;
import me.cortex.voxy.client.config.VoxyConfig;
import me.cortex.voxy.client.core.IGetVoxyRenderSystem;
import me.cortex.voxy.client.core.VoxyRenderSystem;
import me.cortex.voxy.client.core.rendering.RenderDistanceTracker;
import me.cortex.voxy.common.world.WorldEngine;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

import java.util.function.LongConsumer;

/**
 * Every direct reference to a Voxy class lives in this file or the mixin
 * package. Callers MUST check {@link #isVoxyLoaded()} before calling
 * anything else here - the JVM only resolves these class references when a
 * method that touches them actually runs, so gating the call site is what
 * keeps this mod from crashing when Voxy isn't installed.
 */
public final class VoxyBridge {
    private static VoxyRenderSystem lastForceLoadedSystem;
    private static long lastForceLoadCount = -1;

    private VoxyBridge() {
    }

    public static boolean isVoxyLoaded() {
        return FabricLoader.getInstance().isModLoaded("voxy");
    }

    public static long getLastForceLoadCount() {
        return lastForceLoadCount;
    }

    public static boolean hasForceLoadedThisSession() {
        return lastForceLoadedSystem != null;
    }

    public static boolean isRenderSystemActive() {
        return getRenderSystemOrNull() != null;
    }

    /**
     * Guards every other method below on {@link #isVoxyLoaded()} first, so
     * none of them ever touch Voxy's classes (and risk a NoClassDefFoundError)
     * when Voxy isn't on the classpath at all.
     */
    private static VoxyRenderSystem getRenderSystemOrNull() {
        if (!isVoxyLoaded()) {
            return null;
        }
        return IGetVoxyRenderSystem.getNullable();
    }

    /**
     * Called every client tick. Cheap no-op most of the time - only does
     * real work the first tick Voxy's render system becomes available, or
     * after it gets torn down and recreated (e.g. toggling a graphics
     * setting), which is why we compare identity rather than latching a
     * single boolean for the whole session.
     */
    public static void tick() {
        BoundsConfig cfg = BoundsConfig.INSTANCE;
        if (!cfg.enabled) {
            lastForceLoadedSystem = null;
            return;
        }

        VoxyRenderSystem rs = getRenderSystemOrNull();
        if (rs == null) {
            return;
        }

        if (rs != lastForceLoadedSystem) {
            forceLoadBoundingBox(rs);
            lastForceLoadedSystem = rs;
        }

        applyDistances(rs, cfg);
    }

    /** Re-runs the force-load pass immediately, e.g. right after the box is edited via command. */
    public static boolean forceReload() {
        BoundsConfig cfg = BoundsConfig.INSTANCE;
        if (!cfg.enabled) {
            lastForceLoadedSystem = null;
            return false;
        }
        VoxyRenderSystem rs = getRenderSystemOrNull();
        if (rs == null) {
            return false;
        }
        forceLoadBoundingBox(rs);
        lastForceLoadedSystem = rs;
        applyDistances(rs, cfg);
        return true;
    }

    /**
     * Drives the two independent knobs that actually control what's loaded
     * and what's drawn, with a hard split on whether the player is currently
     * inside the box:
     * <ul>
     *   <li>Inside: the ring radius is set to exactly 0 (via
     *   {@code RenderDistanceTracker#setRenderDistance} directly, bypassing
     *   {@code VoxyRenderSystem}'s float wrapper which always pads by +1 and
     *   would otherwise make an exact zero impossible) so the ring never
     *   naturally loads anything outside the box - box nodes are already
     *   force-loaded independently of the ring entirely. The draw cutoff
     *   ({@code VoxyConfig.CONFIG.sectionRenderDistance}) is sized to cover
     *   the farthest box corner from the player, so the *entire* box is
     *   visible regardless of where in it the player is standing.</li>
     *   <li>Outside: the ring radius and draw cutoff both come from the
     *   configured fallback distance (in chunks, converted to Voxy's
     *   512-block node units - see {@link BoundsConfig#fallbackRingSections()}
     *   / {@link BoundsConfig#fallbackCullSections()}), so only a small area
     *   around the player renders. The box itself is not specially kept
     *   visible here: it only shows up if the player happens to be within
     *   that fallback distance of its edge, same as any other terrain.</li>
     * </ul>
     * {@code RenderDistanceTracker#setRenderDistance} and
     * {@code VoxyConfig.CONFIG.sectionRenderDistance} are two genuinely
     * separate knobs Voxy exposes - the former only decides what gets
     * loaded/unloaded (background data management), the latter is what
     * {@code HierarchicalOcclusionTraverser} reads every frame as a
     * squared-distance cutoff baked into a GPU buffer to decide what
     * actually gets drawn. Driving only one of them (as earlier versions of
     * this mod did) left the other still governed by whatever Voxy's own
     * render-distance slider happened to be, which is why the box and the
     * fallback area didn't respect either the box boundary or the fallback
     * distance.
     */
    private static void applyDistances(VoxyRenderSystem rs, BoundsConfig cfg) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }
        double playerX = client.player.getX();
        double playerZ = client.player.getZ();
        boolean insideBox = cfg.isBlockInsideBox(playerX, playerZ);

        RenderDistanceTracker tracker = ((VoxyRenderSystemAccessor) rs).wynnhorizon$getRenderDistanceTracker();
        tracker.setRenderDistance(insideBox ? 0 : cfg.fallbackRingSections());

        VoxyConfig.CONFIG.sectionRenderDistance = insideBox
                ? farthestBoxCornerSections(cfg, playerX, playerZ)
                : cfg.fallbackCullSections();
    }

    /** Node-unit distance from the player to the farthest box corner, plus a section of margin. */
    private static float farthestBoxCornerSections(BoundsConfig cfg, double playerX, double playerZ) {
        double maxDistSq = 0;
        double[] xs = {cfg.minX, cfg.maxX};
        double[] zs = {cfg.minZ, cfg.maxZ};
        for (double x : xs) {
            for (double z : zs) {
                double dx = x - playerX;
                double dz = z - playerZ;
                maxDistSq = Math.max(maxDistSq, dx * dx + dz * dz);
            }
        }
        // +1 section of margin so the farthest corner isn't right at the cutoff edge.
        return (float) (Math.sqrt(maxDistSq) / 512.0) + 1.0f;
    }

    private static void forceLoadBoundingBox(VoxyRenderSystem rs) {
        RenderDistanceTracker tracker = ((VoxyRenderSystemAccessor) rs).wynnhorizon$getRenderDistanceTracker();
        RenderDistanceTrackerAccessor acc = (RenderDistanceTrackerAccessor) tracker;
        LongConsumer addTopLevelNode = acc.wynnhorizon$getAddTopLevelNode();
        int minSec = acc.wynnhorizon$getMinSec();
        int maxSec = acc.wynnhorizon$getMaxSec();

        BoundsConfig cfg = BoundsConfig.INSTANCE;
        int nodeMinX = cfg.nodeMinX();
        int nodeMaxX = cfg.nodeMaxX();
        int nodeMinZ = cfg.nodeMinZ();
        int nodeMaxZ = cfg.nodeMaxZ();

        long count = 0;
        for (int x = nodeMinX; x <= nodeMaxX; x++) {
            for (int z = nodeMinZ; z <= nodeMaxZ; z++) {
                for (int y = minSec; y <= maxSec; y++) {
                    addTopLevelNode.accept(WorldEngine.getWorldSectionId(4, x, y, z));
                    count++;
                }
            }
        }

        lastForceLoadCount = count;
        WynnHorizonMod.LOGGER.info(
                "Force-loaded {} Voxy top-level nodes covering bounding box x[{}..{}] z[{}..{}] (node units, y[{}..{}])",
                count, nodeMinX, nodeMaxX, nodeMinZ, nodeMaxZ, minSec, maxSec);
    }
}
