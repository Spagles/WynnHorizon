package dev.wynnhorizon.mixin;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Every mixin in this config targets Voxy classes. If Voxy isn't installed
 * those classes don't exist, so Mixin must never even attempt to load/apply
 * them - otherwise the game would crash on startup instead of this mod just
 * being a no-op (requirement: fail gracefully without Voxy present).
 */
public class VoxyMixinPlugin implements IMixinConfigPlugin {
    private boolean voxyPresent;

    @Override
    public void onLoad(String mixinPackage) {
        this.voxyPresent = FabricLoader.getInstance().isModLoaded("voxy");
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return this.voxyPresent;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
