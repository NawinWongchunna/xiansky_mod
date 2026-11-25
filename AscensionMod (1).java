package com.ascension.rings;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AscensionMod implements ModInitializer {
    public static final String MOD_ID = "ascension_rings";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        // Client pressed the skill key. Always handled on the server thread.
        ServerPlayNetworking.registerGlobalReceiver(AscensionNetworking.ACTIVATE,
                (server, player, handler, buf, responseSender) ->
                        server.execute(() -> AscensionManager.tryStart(player)));

        ServerTickEvents.END_SERVER_TICK.register(AscensionManager::tick);

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                AscensionManager.cleanup(handler.getPlayer()));

        LOGGER.info("Ascension Rings loaded");
    }
}
