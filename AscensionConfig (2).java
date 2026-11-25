package com.ascension.rings;

/**
 * Every tunable value lives here. Times are in game ticks (20 ticks = 1 second).
 * Shared by the server (ability logic) and the client (VFX / camera timeline),
 * so both sides always agree on the timeline.
 */
public final class AscensionConfig {
    private AscensionConfig() {}

    // ---- Timeline (ticks since activation) ----
    /** Player rises quickly until this tick. */
    public static final int RISE_END = 60;
    /** Player hovers and the vortex drains the atmosphere until this tick. */
    public static final int ABSORB_END = 110;
    /** Release (blast) happens on this tick. Between ABSORB_END and here is the "peak" build-up. */
    public static final int RELEASE_TICK = 140;
    /** Whole ability length. After this the player drops with slow falling. */
    public static final int TOTAL_TICKS = 170;

    // ---- Movement ----
    /** Blocks per tick during the rise phase (0.2 * 60 ticks = 12 blocks). */
    public static final double RISE_SPEED = 0.20;
    /** Blocks per tick while draining (slow drift upward). */
    public static final double ABSORB_SPEED = 0.03;
    /** Slow Falling duration after the ability ends (ticks). */
    public static final int SLOW_FALL_TICKS = 220;

    // ---- Gameplay ----
    /** Cooldown after the ability ends (ticks). 600 = 30 seconds. */
    public static final int COOLDOWN_TICKS = 600;
    public static final double BLAST_RADIUS = 9.0;
    public static final float BLAST_DAMAGE = 24.0f;
    public static final int BLAST_FIRE_SECONDS = 5;
    public static final boolean DAMAGE_PLAYERS = false;

    // ---- Client ----
    /** Take over the camera (cinematic cutscene). Set false for a plain first-person view. */
    public static final boolean CUTSCENE_ENABLED = true;
    /** Lock mouse look and movement keys while the ability runs. */
    public static final boolean LOCK_INPUT = true;
    /** Camera shake multiplier (0 = off). */
    public static final float SHAKE_SCALE = 1.0f;
    /** Max distance (blocks) at which other players' effects are drawn. */
    public static final double VFX_RENDER_DISTANCE = 96.0;
}
