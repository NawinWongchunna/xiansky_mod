package dev.ascension.rings;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

/** config/ascension_rings.json */
public final class AscensionConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static AscensionConfig instance = new AscensionConfig();

    /** เปิด cutscene (กล้อง/ฟ้าเปลี่ยนสี/แถบดำ/overlay) ฝั่ง client */
    public boolean cinematic = true;
    /** ดาเมจตอนปล่อยพลัง (ฝั่ง server) */
    public float damage = 14.0f;
    /** รัศมีดาเมจ (บล็อก) */
    public double radius = 14.0;
    /** คูลดาวน์ (tick) 400 = 20 วินาที */
    public int cooldownTicks = 400;

    public static AscensionConfig get() {
        return instance;
    }

    public static void load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("ascension_rings.json");
        try {
            if (Files.exists(path)) {
                AscensionConfig loaded = GSON.fromJson(Files.readString(path), AscensionConfig.class);
                if (loaded != null) instance = loaded;
            }
            Files.writeString(path, GSON.toJson(instance));
        } catch (Exception e) {
            AscensionMod.LOGGER.warn("Could not read/write config, using defaults", e);
        }
    }
}
