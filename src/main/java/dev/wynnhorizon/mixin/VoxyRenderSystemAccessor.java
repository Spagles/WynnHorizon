package dev.wynnhorizon.mixin;

import me.cortex.voxy.client.core.VoxyRenderSystem;
import me.cortex.voxy.client.core.rendering.RenderDistanceTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = VoxyRenderSystem.class, remap = false)
public interface VoxyRenderSystemAccessor {
    @Accessor("renderDistanceTracker")
    RenderDistanceTracker wynnhorizon$getRenderDistanceTracker();
}
