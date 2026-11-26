package dev.ascension.rings.network;

import dev.ascension.rings.AscensionMod;
import net.minecraft.util.Identifier;

public final class Net {
    /** client -> server : ผู้เล่นกดปุ่มสกิล */
    public static final Identifier ACTIVATE = new Identifier(AscensionMod.MOD_ID, "activate");
    /** server -> client : เริ่มสกิล (uuid, x, y, z) */
    public static final Identifier START = new Identifier(AscensionMod.MOD_ID, "start");
    /** server -> client : จบสกิล (uuid) */
    public static final Identifier END = new Identifier(AscensionMod.MOD_ID, "end");

    private Net() {}
}
