package dev.ascension.rings;

import dev.ascension.rings.ability.AscensionServer;
import dev.ascension.rings.ability.SwordServer;
import dev.ascension.rings.network.Net;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.command.CommandManager;
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

        ServerPlayNetworking.registerGlobalReceiver(Net.SWORD_ACTIVATE,
                (server, player, handler, buf, responseSender) ->
                        server.execute(() -> SwordServer.start(player)));

        ServerTickEvents.END_SERVER_TICK.register(AscensionServer::tick);
        ServerTickEvents.END_SERVER_TICK.register(SwordServer::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            AscensionServer.abort(handler.player);
            SwordServer.abort(handler.player);
        });

        // /ascension  = ใช้สกิลโดยไม่ต้องกดปุ่ม (ไว้ทดสอบ)
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("ascension").executes(ctx -> {
                    AscensionServer.start(ctx.getSource().getPlayerOrThrow());
                    return 1;
                })));

        // /voidblade = ใช้สกิล G โดยไม่ต้องกดปุ่ม (ไว้ทดสอบ)
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("voidblade").requires(src -> src.hasPermissionLevel(2)).executes(ctx -> {
                    SwordServer.start(ctx.getSource().getPlayerOrThrow());
                    return 1;
                })));

        LOGGER.info("Ascension Rings loaded");
    }
}
