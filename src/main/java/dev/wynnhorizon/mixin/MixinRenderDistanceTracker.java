package dev.wynnhorizon.mixin;

import dev.wynnhorizon.config.BoundsConfig;
import me.cortex.voxy.client.core.rendering.RenderDistanceTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * {@code rem(x, z)} is called by the ring tracker whenever a top-level node
 * falls outside the player-radius ring - including nodes we've force-loaded
 * as part of the always-on bounding box. Cancelling it here is what makes
 * those nodes actually stay loaded regardless of player distance; the ring's
 * own bookkeeping doesn't care whether the callback did anything, so this is
 * safe to no-op.
 *
 * {@code add(x, z)} is also cancelled for box nodes: the ring's own radius
 * naturally sweeps back over force-loaded box positions as the player moves
 * (it has no idea we already loaded them directly via the accessor, since
 * that bypassed its internal bookkeeping entirely), and re-running the
 * callback for an already-active node just makes Voxy's NodeManager log an
 * "already in active map, discarding" error every time. Since box nodes are
 * loaded exactly once at force-load time and never removed, there is never
 * a legitimate reason for the ring to (re-)add one.
 */
@Mixin(value = RenderDistanceTracker.class, remap = false)
public class MixinRenderDistanceTracker {
    @Inject(method = "rem", at = @At("HEAD"), cancellable = true)
    private void wynnhorizon$keepBoxNodesLoaded(int x, int z, CallbackInfo ci) {
        BoundsConfig cfg = BoundsConfig.INSTANCE;
        if (cfg.enabled && cfg.isNodeInsideBox(x, z)) {
            ci.cancel();
        }
    }

    @Inject(method = "add", at = @At("HEAD"), cancellable = true)
    private void wynnhorizon$skipRedundantBoxAdd(int x, int z, CallbackInfo ci) {
        BoundsConfig cfg = BoundsConfig.INSTANCE;
        if (cfg.enabled && cfg.isNodeInsideBox(x, z)) {
            ci.cancel();
        }
    }
}
