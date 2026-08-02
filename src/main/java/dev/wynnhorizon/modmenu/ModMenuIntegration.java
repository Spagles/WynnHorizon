package dev.wynnhorizon.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.wynnhorizon.config.BoundsConfig;
import dev.wynnhorizon.voxy.VoxyBridge;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Registered under the "modmenu" entrypoint in fabric.mod.json. Only ever
 * touched by ModMenu's own code (it looks up entrypoints registered under
 * that key and calls this), so this class is never classloaded - and this
 * file's direct Cloth Config/ModMenu references never resolved - unless
 * ModMenu is actually installed. Cloth Config isn't separately guarded
 * because Cloth Config's own presence is what ModMenu screens generally
 * require; if a player has ModMenu but not Cloth Config, ModMenu itself
 * won't offer this screen.
 * <p>
 * The map bounds live in their own "Advanced" category with an explicit
 * warning: unlike the General settings (which take effect the moment the
 * screen is saved), shrinking the box doesn't retroactively unload terrain
 * that was already force-loaded under the old, larger box - that only
 * happens on rejoin. See BoundsConfig for the field-level detail.
 */
public final class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ModMenuIntegration::buildScreen;
    }

    private static Screen buildScreen(Screen parent) {
        BoundsConfig cfg = BoundsConfig.INSTANCE;

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.literal("WynnHorizon"))
                .setSavingRunnable(() -> {
                    cfg.save();
                    if (VoxyBridge.isVoxyLoaded()) {
                        VoxyBridge.forceReload();
                    }
                });

        ConfigEntryBuilder entry = builder.entryBuilder();
        ConfigCategory general = builder.getOrCreateCategory(Component.literal("General"));

        general.addEntry(entry.startBooleanToggle(Component.literal("Enabled"), cfg.enabled)
                .setDefaultValue(true)
                .setTooltip(Component.literal("Turns the whole mod on or off. Applies immediately."))
                .setSaveConsumer(value -> cfg.enabled = value)
                .build());

        general.addEntry(entry.startFloatField(Component.literal("Outside Render Distance"), cfg.fallbackRenderDistance)
                .setMin(0f)
                .setTooltip(Component.literal("Render distance, in chunks, used outside the map. Applies immediately."))
                .setSaveConsumer(value -> cfg.fallbackRenderDistance = value)
                .build());

        general.addEntry(entry.startBooleanToggle(Component.literal("Show Debug HUD"), cfg.showHud)
                .setDefaultValue(false)
                .setTooltip(Component.literal("Shows a small on-screen readout of the mod's live state. Applies immediately."))
                .setSaveConsumer(value -> cfg.showHud = value)
                .build());

        ConfigCategory advanced = builder.getOrCreateCategory(Component.literal("Advanced"));
        Component growWarning = Component.literal(
                "Advanced setting - only change this if you know what you're doing. Growing the box "
                        + "force-loads the new area as soon as you save. Shrinking it does NOT unload terrain "
                        + "already loaded under the old, larger box - that only happens after you rejoin the world.");

        advanced.addEntry(entry.startIntField(Component.literal("Min X"), cfg.minX)
                .setTooltip(growWarning)
                .setSaveConsumer(value -> cfg.minX = value)
                .build());
        advanced.addEntry(entry.startIntField(Component.literal("Max X"), cfg.maxX)
                .setTooltip(growWarning)
                .setSaveConsumer(value -> cfg.maxX = value)
                .build());
        advanced.addEntry(entry.startIntField(Component.literal("Min Z"), cfg.minZ)
                .setTooltip(growWarning)
                .setSaveConsumer(value -> cfg.minZ = value)
                .build());
        advanced.addEntry(entry.startIntField(Component.literal("Max Z"), cfg.maxZ)
                .setTooltip(growWarning)
                .setSaveConsumer(value -> cfg.maxZ = value)
                .build());

        return builder.build();
    }
}
