package dev.wynnhorizon.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.wynnhorizon.config.BoundsConfig;
import dev.wynnhorizon.voxy.VoxyBridge;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.network.chat.Component;

/**
 * {@code /wynnhorizon ...} - the small, deliberately minimal chat-command
 * surface for the two things worth a quick command instead of opening the
 * Mod Menu config screen: turning the mod off, and tuning the outside-map
 * render distance. Everything else (map bounds, the debug HUD) lives in the
 * Mod Menu screen only - see {@code ModMenuIntegration}.
 */
public final class BoundsCommand {
    private BoundsCommand() {
    }

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandBuildContext buildContext) {
        dispatcher.register(ClientCommandManager.literal("wynnhorizon")
                .then(ClientCommandManager.literal("toggle").executes(BoundsCommand::toggle))
                .then(ClientCommandManager.literal("setoutsiderender")
                        .then(ClientCommandManager.argument("value", DoubleArgumentType.doubleArg(0))
                                .executes(BoundsCommand::setOutsideRender)))
        );
    }

    private static int toggle(CommandContext<FabricClientCommandSource> ctx) {
        BoundsConfig cfg = BoundsConfig.INSTANCE;
        cfg.enabled = !cfg.enabled;
        cfg.save();
        ctx.getSource().sendFeedback(Component.literal("WynnHorizon " + (cfg.enabled ? "enabled" : "disabled")));
        return 1;
    }

    private static int setOutsideRender(CommandContext<FabricClientCommandSource> ctx) {
        double value = DoubleArgumentType.getDouble(ctx, "value");
        BoundsConfig cfg = BoundsConfig.INSTANCE;
        cfg.fallbackRenderDistance = (float) value;
        cfg.save();
        if (VoxyBridge.isVoxyLoaded()) {
            VoxyBridge.forceReload();
        }
        ctx.getSource().sendFeedback(Component.literal("Outside-map render distance set to " + value + " chunks"));
        return 1;
    }
}
