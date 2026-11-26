package dev.ascension.rings.client;

import dev.ascension.rings.client.vfx.AscensionHud;
import dev.ascension.rings.client.vfx.AscensionRenderer;
import dev.ascension.rings.network.Net;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import dev.ascension.rings.AscensionMod;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.UUID;

public class AscensionClient implements ClientModInitializer {
    /** ปุ่มสกิล = P (เปลี่ยนได้ใน Options > Controls) */
    public static KeyBinding ACTIVATE_KEY;

    @Override
    public void onInitializeClient() {
        ACTIVATE_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.ascension_rings.activate",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_P,
                "key.categories.ascension_rings"));

        ClientPlayNetworking.registerGlobalReceiver(Net.START, (client, handler, buf, sender) -> {
            UUID id = buf.readUuid();
            Vec3d pos = new Vec3d(buf.readDouble(), buf.readDouble(), buf.readDouble());
            client.execute(() -> ClientState.start(id, pos));
        });
        ClientPlayNetworking.registerGlobalReceiver(Net.END, (client, handler, buf, sender) -> {
            UUID id = buf.readUuid();
            client.execute(() -> ClientState.stop(id));
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            freeDefaultKey(client);
            while (ACTIVATE_KEY.wasPressed()) {
                AscensionMod.LOGGER.info("Ascension key pressed (screen={}, active={})",
                        client.currentScreen, ClientState.isLocalActive());
                if (client.player != null && client.currentScreen == null && !ClientState.isLocalActive()) {
                    ClientPlayNetworking.send(Net.ACTIVATE, PacketByteBufs.create());
                }
            }
            ClientState.tick(client);
        });

        WorldRenderEvents.AFTER_TRANSLUCENT.register(AscensionRenderer::render);
        HudRenderCallback.EVENT.register(AscensionHud::render);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientState.clear());
    }

    private static boolean keyFreed = false;

    /**
     * vanilla ผูก P ไว้กับ Social Interactions: ถ้าสองปุ่มชนกัน Minecraft ส่งการกดให้แค่ปุ่มเดียว
     * จึงย้าย Social Interactions ออก (ทำครั้งเดียว และเฉพาะตอนที่ยังเป็น P ค่าเริ่มต้น)
     */
    private static void freeDefaultKey(net.minecraft.client.MinecraftClient client) {
        if (keyFreed) return;
        keyFreed = true;
        KeyBinding social = client.options.socialInteractionsKey;
        if (social.getDefaultKey().getCode() == GLFW.GLFW_KEY_P
                && social.equals(ACTIVATE_KEY) == false
                && social.getBoundKeyTranslationKey().equals(ACTIVATE_KEY.getBoundKeyTranslationKey())) {
            social.setBoundKey(InputUtil.UNKNOWN_KEY);
            KeyBinding.updateKeysByCode();
            client.options.write();
            AscensionMod.LOGGER.info("Unbound Social Interactions from P so the Ascension key works");
        }
    }
}
