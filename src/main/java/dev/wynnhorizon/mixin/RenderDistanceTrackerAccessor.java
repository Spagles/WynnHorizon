package dev.wynnhorizon.mixin;

import me.cortex.voxy.client.core.rendering.RenderDistanceTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.function.LongConsumer;

/**
 * Voxy has no public API for adding/removing individual top-level LOD
 * nodes - {@code nodeManager} and {@code renderDistanceTracker} are both
 * private. This exposes the two node-callbacks and the vertical section
 * range straight off {@link RenderDistanceTracker} so {@code VoxyBridge}
 * can force-load the configured bounding box directly, bypassing the ring
 * math entirely.
 */
@Mixin(value = RenderDistanceTracker.class, remap = false)
public interface RenderDistanceTrackerAccessor {
    @Accessor("addTopLevelNode")
    LongConsumer wynnhorizon$getAddTopLevelNode();

    @Accessor("removeTopLevelNode")
    LongConsumer wynnhorizon$getRemoveTopLevelNode();

    @Accessor("minSec")
    int wynnhorizon$getMinSec();

    @Accessor("maxSec")
    int wynnhorizon$getMaxSec();
}
