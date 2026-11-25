package com.ascension.rings;

import com.ascension.rings.mixin.ServerPlayNetworkHandlerAccessor;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Server-side state machine for the Ascension ability. */
public final class AscensionManager {
    private AscensionManager() {}

    private static final class Session {
        int tick = 0;
        boolean released = false;
        final Vec3d origin;

        Session(Vec3d origin) {
            this.origin = origin;
        }
    }

    private static final Map<UUID, Session> ACTIVE = new HashMap<>();
    /** Server game time (in ticks) at which each player's cooldown ends. */
    private static final Map<UUID, Long> COOLDOWN_UNTIL = new HashMap<>();

    public static boolean isActive(UUID id) {
        return ACTIVE.containsKey(id);
    }

    public static void tryStart(ServerPlayerEntity player) {
        if (player.isSpectator() || !player.isAlive() || player.hasVehicle()) return;
        if (ACTIVE.containsKey(player.getUuid())) return;

        long now = player.getServerWorld().getTime();
        long until = COOLDOWN_UNTIL.getOrDefault(player.getUuid(), 0L);
        if (now < until) {
            long left = (until - now + 19) / 20;
            player.sendMessage(Text.literal("Ascension cooldown: " + left + "s"), true);
            return;
        }

        Session session = new Session(player.getPos());
        ACTIVE.put(player.getUuid(), session);
        player.setNoGravity(true);
        player.fallDistance = 0;

        player.getServerWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 2.0f, 0.6f);
        broadcast(player, AscensionNetworking.TYPE_START, session.origin);
    }

    public static void tick(MinecraftServer server) {
        Iterator<Map.Entry<UUID, Session>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Session> entry = it.next();
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
            Session s = entry.getValue();

            if (player == null || !player.isAlive() || player.isSpectator()) {
                if (player != null) finish(player, false);
                it.remove();
                continue;
            }

            s.tick++;
            player.fallDistance = 0;
            player.setNoGravity(true);

            // Stop vanilla's anti-fly kick while hovering
            ServerPlayNetworkHandlerAccessor acc = (ServerPlayNetworkHandlerAccessor) player.networkHandler;
            acc.ascension$setFloatingTicks(0);
            acc.ascension$setVehicleFloatingTicks(0);

            double vy;
            if (s.tick <= AscensionConfig.RISE_END) {
                // ease-out so the take-off feels heavy then smooth
                double t = s.tick / (double) AscensionConfig.RISE_END;
                vy = AscensionConfig.RISE_SPEED * (1.15 - 0.6 * t);
            } else if (s.tick <= AscensionConfig.ABSORB_END) {
                vy = AscensionConfig.ABSORB_SPEED;
            } else {
                vy = 0.0;
            }
            Vec3d v = player.getVelocity();
            player.setVelocity(v.x * 0.5, vy, v.z * 0.5);
            player.velocityModified = true;

            ServerWorld world = player.getServerWorld();

            // Ambient particles while charging
            if (s.tick % 2 == 0 && s.tick < AscensionConfig.RELEASE_TICK) {
                world.spawnParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 0.1, player.getZ(),
                        4, 0.6, 0.1, 0.6, 0.02);
            }
            if (s.tick == AscensionConfig.RISE_END) {
                world.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BLOCK_END_PORTAL_SPAWN, SoundCategory.PLAYERS, 1.0f, 1.4f);
            }

            if (!s.released && s.tick >= AscensionConfig.RELEASE_TICK) {
                s.released = true;
                release(player, world);
            }

            if (s.tick >= AscensionConfig.TOTAL_TICKS) {
                finish(player, true);
                it.remove();
            }
        }
    }

    private static void release(ServerPlayerEntity player, ServerWorld world) {
        Vec3d c = player.getPos();
        world.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y, c.z, 1, 0, 0, 0, 0);
        world.spawnParticles(ParticleTypes.LAVA, c.x, c.y, c.z, 60, 3.0, 1.0, 3.0, 0.1);
        world.playSound(null, c.x, c.y, c.z, SoundEvents.ENTITY_GENERIC_EXPLODE,
                SoundCategory.PLAYERS, 4.0f, 0.5f);
        world.playSound(null, c.x, c.y, c.z, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER,
                SoundCategory.PLAYERS, 3.0f, 0.8f);

        double r = AscensionConfig.BLAST_RADIUS;
        Box box = new Box(c.x - r, c.y - r - 12, c.z - r, c.x + r, c.y + r, c.z + r);
        DamageSource src = player.getDamageSources().playerAttack(player);
        for (LivingEntity e : world.getEntitiesByClass(LivingEntity.class, box, x -> x != player && x.isAlive())) {
            if (e instanceof PlayerEntity && !AscensionConfig.DAMAGE_PLAYERS) continue;
            double dx = e.getX() - c.x;
            double dz = e.getZ() - c.z;
            if (dx * dx + dz * dz > r * r) continue;
            e.damage(src, AscensionConfig.BLAST_DAMAGE);
            e.setOnFireFor(AscensionConfig.BLAST_FIRE_SECONDS);
            Vec3d push = new Vec3d(dx, 0, dz);
            if (push.lengthSquared() > 1.0E-4) {
                push = push.normalize().multiply(1.2);
                e.addVelocity(push.x, 0.5, push.z);
                e.velocityModified = true;
            }
        }
    }

    private static void finish(ServerPlayerEntity player, boolean applyEffects) {
        player.setNoGravity(false);
        player.fallDistance = 0;
        if (applyEffects) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING,
                    AscensionConfig.SLOW_FALL_TICKS, 0, false, false, true));
            COOLDOWN_UNTIL.put(player.getUuid(),
                    player.getServerWorld().getTime() + AscensionConfig.COOLDOWN_TICKS);
        }
        broadcast(player, AscensionNetworking.TYPE_END, player.getPos());
    }

    public static void cleanup(ServerPlayerEntity player) {
        if (ACTIVE.remove(player.getUuid()) != null) {
            player.setNoGravity(false);
        }
    }

    private static void broadcast(ServerPlayerEntity player, byte type, Vec3d pos) {
        Set<ServerPlayerEntity> targets = new HashSet<>(PlayerLookup.tracking(player));
        targets.add(player);
        for (ServerPlayerEntity target : targets) {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeByte(type);
            buf.writeVarInt(player.getId());
            buf.writeDouble(pos.x);
            buf.writeDouble(pos.y);
            buf.writeDouble(pos.z);
            ServerPlayNetworking.send(target, AscensionNetworking.STATE, buf);
        }
    }
}
