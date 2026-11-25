package com.ascension.rings;

import net.minecraft.util.Identifier;

public final class AscensionNetworking {
    private AscensionNetworking() {}

    /** Client -> server: "I pressed the skill key". Empty payload. */
    public static final Identifier ACTIVATE = new Identifier(AscensionMod.MOD_ID, "activate");

    /**
     * Server -> clients: ability state change.
     * Payload: byte type, varInt entityId, double x, double y, double z (ground position at start).
     */
    public static final Identifier STATE = new Identifier(AscensionMod.MOD_ID, "state");

    public static final byte TYPE_START = 0;
    public static final byte TYPE_END = 1;
}
