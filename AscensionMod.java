package dev.ascension.rings;

import dev.ascension.rings.ability.AscensionServer;
import dev.ascension.rings.network.Net;
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
        AscensionConfig.load();

        ServerPlayNetworking.registerGlobalReceiver(Net.ACTIVATE,
                (server, player, handler, buf, responseSender) ->
                        server.execute(() -> AscensionServer.start(player)));

        ServerTickEvents.END_SERVER_TICK.register(AscensionServer::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> AscensionServer.abort(handler.player));

        LOGGER.info("Ascension Rings loaded");
    }
}
