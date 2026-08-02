package dev.wynnhorizon;

import dev.wynnhorizon.command.BoundsCommand;
import dev.wynnhorizon.debug.BoundsHud;
import dev.wynnhorizon.voxy.VoxyBridge;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WynnHorizonMod implements ClientModInitializer {
    public static final String MOD_ID = "wynnhorizon";
    public static final Logger LOGGER = LoggerFactory.getLogger("WynnHorizon");

    @Override
    public void onInitializeClient() {
        if (!VoxyBridge.isVoxyLoaded()) {
            LOGGER.warn("Voxy was not found - WynnHorizon has nothing to hook into and will stay idle. "
                    + "Install Voxy (https://modrinth.com/mod/voxy) to use this mod.");
        } else {
            LOGGER.info("Voxy detected - bounding box enforcement active.");
            ClientTickEvents.END_CLIENT_TICK.register(client -> VoxyBridge.tick());
        }

        BoundsHud.register();
        ClientCommandRegistrationCallback.EVENT.register(BoundsCommand::register);
    }
}
