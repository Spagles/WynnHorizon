package dev.wynnhorizon.debug;

import dev.wynnhorizon.config.BoundsConfig;
import dev.wynnhorizon.voxy.VoxyBridge;
import me.cortex.voxy.client.config.VoxyConfig;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CommonColors;

import java.util.ArrayList;
import java.util.List;

/**
 * Small always-available on-screen readout so the bounding-box behaviour can
 * be verified live in-game: enable via Mod Menu or the config file, fly to
 * the configured edge and watch the "inside/outside box" line and node count
 * change as you cross it.
 */
public final class BoundsHud {
    private static final Identifier ID = Identifier.fromNamespaceAndPath("wynnhorizon", "debug_hud");

    private BoundsHud() {
    }

    public static void register() {
        HudElementRegistry.attachElementAfter(VanillaHudElements.SUBTITLES, ID, BoundsHud::render);
    }

    private static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        BoundsConfig cfg = BoundsConfig.INSTANCE;
        if (!cfg.showHud) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }

        List<String> lines = new ArrayList<>();
        lines.add("WynnHorizon: " + (cfg.enabled ? "ENABLED" : "disabled"));

        if (!VoxyBridge.isVoxyLoaded()) {
            lines.add("Voxy: NOT INSTALLED");
        } else if (!VoxyBridge.isRenderSystemActive()) {
            lines.add("Voxy: detected, render system not active yet");
        } else {
            int x = (int) Math.floor(client.player.getX());
            int z = (int) Math.floor(client.player.getZ());
            boolean inside = cfg.isBlockInsideBox(x, z);

            lines.add("Player block: " + x + ", " + z + "  (node " + (x >> 9) + ", " + (z >> 9) + ")");
            lines.add(inside
                    ? "Inside main-map box: box only, ring radius 0"
                    : "Outside main-map box: fallback radius " + cfg.fallbackRenderDistance + " chunks");

            if (!inside) {
                int distX = Math.max(cfg.minX - x, Math.max(0, x - cfg.maxX));
                int distZ = Math.max(cfg.minZ - z, Math.max(0, z - cfg.maxZ));
                lines.add("Distance outside box: x=" + distX + " z=" + distZ);
            }

            lines.add("Box: x[" + cfg.minX + ".." + cfg.maxX + "] z[" + cfg.minZ + ".." + cfg.maxZ + "]");
            lines.add("Force-loaded top-level nodes: "
                    + (VoxyBridge.hasForceLoadedThisSession() ? String.valueOf(VoxyBridge.getLastForceLoadCount()) : "not yet run"));
            lines.add("Voxy sectionRenderDistance (actual draw cutoff, node units): " + VoxyConfig.CONFIG.sectionRenderDistance);
        }

        int y = 4;
        for (String line : lines) {
            int width = client.font.width(line);
            graphics.fill(2, y - 1, 6 + width, y + 9, 0x90000000);
            graphics.drawString(client.font, line, 4, y, CommonColors.WHITE);
            y += 10;
        }
    }
}
