package com.xiansky;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class XianSkyClient implements ClientModInitializer {

    // ================= สกิล 1 (G): เดินบนอากาศ + วงแหวนพุ่ง =================
    static final int    DURATION     = 300;   // ติก (300 = 15 วิ)
    static final double RING_DIST    = 5.0;
    static final double RING_RADIUS  = 1.4;
    static final int    DASH_TICKS   = 14;
    static final double DASH_SPEED   = 1.1;
    static final float  BIG_RING     = 1.6f;
    static final float  OUTER_RING   = 2.6f;

    // ================= สกิล 2 (H): หยินหยางสายฟ้าเพลิง =================
    static final int   S2_CHARGE   = 20;   // ติกที่ใช้ชาร์จก่อนสายฟ้าฟาด
    static final int   S2_STRIKES  = 12;   // จำนวนสายฟ้า (ฟาดทุก 3 ติก)
    static final int   S2_COOLDOWN = 40;   // คูลดาวน์หลังจบ (ติก)
    static final float BOLT_HEIGHT = 16f;  // ความสูงสายฟ้า (บล็อก)

    // อัตราส่วนกว้าง/สูงของรูป (หลังครอป)
    static final float BOLT_ASPECT  = 0.281f;
    static final float FLARE_ASPECT = 0.809f;
    static final float FIRE_ASPECT  = 0.844f;

    static final Identifier TEX_RING  = new Identifier("xiansky", "textures/vfx/ring.png");
    static final Identifier TEX_OUTER = new Identifier("xiansky", "textures/vfx/outer.png");
    static final Identifier TEX_BOLT  = new Identifier("xiansky", "textures/vfx/bolt.png");
    static final Identifier TEX_FLARE = new Identifier("xiansky", "textures/vfx/flare.png");
    static final Identifier TEX_FIRE  = new Identifier("xiansky", "textures/vfx/fire.png");
    static final Identifier TEX_TAIJI = new Identifier("xiansky", "textures/vfx/taiji.png");

    // ----- สถานะสกิล 1 -----
    static boolean active = false;
    static double anchorY;
    static int ticks, dash;
    static Vec3d dashDir = Vec3d.ZERO;
    static Ring targetRing, targetOuter;

    // ----- สถานะสกิล 2 -----
    static boolean s2Active = false;
    static int s2Ticks, s2Cool;
    static Vec3d s2Origin = Vec3d.ZERO, s2Dir = new Vec3d(0, 0, 1);
    static final Random rnd = new Random();

    static final List<Ring> rings = new ArrayList<>();
    static KeyBinding skillKey, skill2Key;

    /** เอฟเฟกต์หนึ่งชิ้น: แบนราบกับพื้น (flat) หรือตั้งตรงหันเข้ากล้อง (upright) */
    static class Ring {
        final Identifier tex;
        final boolean upright;
        final double x, y, z;
        final float w, h, spin;
        float r = 0.75f, g = 0.95f, b = 1f;
        float rise = 0f, s0 = 1f, s1 = 1f;
        boolean flicker = false;
        int age = 0, max;

        Ring(Identifier tex, double x, double y, double z, float half, float spin, int max) {
            this(tex, false, x, y, z, half, half, spin, max);
        }
        Ring(Identifier tex, boolean upright, double x, double y, double z, float w, float h, float spin, int max) {
            this.tex = tex; this.upright = upright; this.x = x; this.y = y; this.z = z;
            this.w = w; this.h = h; this.spin = spin; this.max = max;
        }
        Ring tint(float r, float g, float b) { this.r = r; this.g = g; this.b = b; return this; }
        Ring rise(float v) { this.rise = v; return this; }
        Ring scale(float a, float b) { this.s0 = a; this.s1 = b; return this; }
        Ring flicker() { this.flicker = true; return this; }
    }

    @Override
    public void onInitializeClient() {
        skillKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.xiansky.skill", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, "key.categories.xiansky"));
        skill2Key = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.xiansky.skill2", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_H, "key.categories.xiansky"));

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
        WorldRenderEvents.AFTER_TRANSLUCENT.register(this::onRender);
    }

    // ================= ลอจิกหลัก =================
    private void onTick(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null) { active = false; s2Active = false; rings.clear(); return; }

        for (Iterator<Ring> it = rings.iterator(); it.hasNext(); ) {
            Ring r = it.next();
            if (++r.age >= r.max) it.remove();
        }

        tickSkill2(mc, p);   // สกิล 2 ทำงานอิสระจากสกิล 1

        while (skillKey.wasPressed()) {
            if (active) stop(p); else start(p);
        }
        if (!active) return;

        ticks++;
        if (ticks > DURATION || (p.isSneaking() && ticks > 10)) { stop(p); return; }

        if (dash > 0) {
            p.setVelocity(dashDir.x * DASH_SPEED, 0, dashDir.z * DASH_SPEED);
            if (dash % 2 == 0) spawnPair(p.getX(), anchorY + 0.05, p.getZ(), 0.9f, 1.8f, 22);
            dash--;
            if (p.horizontalCollision) dash = 0;
            if (dash == 0) placeTarget(p, RING_DIST + 2);
        }

        if (p.getY() < anchorY) {
            p.setPosition(p.getX(), anchorY, p.getZ());
            Vec3d v = p.getVelocity();
            if (v.y < 0) p.setVelocity(v.x, 0, v.z);
        }
        if (p.getY() <= anchorY + 0.01) p.setOnGround(true);
        p.fallDistance = 0;

        if (dash == 0 && ticks % 8 == 0 && p.getVelocity().horizontalLengthSquared() > 0.0004) {
            rings.add(new Ring(TEX_RING, p.getX(), anchorY + 0.03, p.getZ(), 0.8f, 100f, 20));
        }

        if (targetRing != null && dash == 0) {
            double dx = p.getX() - targetRing.x, dz = p.getZ() - targetRing.z;
            if (Math.hypot(dx, dz) < RING_RADIUS) {
                Vec3d look = p.getRotationVec(1.0f);
                double l = Math.hypot(look.x, look.z);
                dashDir = l < 1e-4 ? Vec3d.ZERO : new Vec3d(look.x / l, 0, look.z / l);
                dash = DASH_TICKS;
                targetRing.max = targetRing.age + 10;
                targetOuter.max = targetOuter.age + 10;
                targetRing = null; targetOuter = null;
                spawnPair(p.getX(), anchorY + 0.05, p.getZ(), BIG_RING, OUTER_RING, 26);
                p.playSound(SoundEvents.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 1.3f);
            }
        }
    }

    // ================= สกิล 1 =================
    private void start(ClientPlayerEntity p) {
        active = true;
        anchorY = p.getY();
        ticks = 0; dash = 0;
        placeTarget(p, RING_DIST);
        spawnPair(p.getX(), anchorY + 0.05, p.getZ(), BIG_RING, OUTER_RING, 30);
        p.playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, 0.8f, 1.4f);
        p.sendMessage(Text.literal("§b天 เดินบนอากาศ §7(Shift = ยกเลิก)"), true);
    }

    private void stop(ClientPlayerEntity p) {
        active = false; dash = 0;
        if (targetRing != null) { targetRing.max = targetRing.age + 10; targetOuter.max = targetOuter.age + 10; }
        targetRing = null; targetOuter = null;
        p.sendMessage(Text.literal("§7สกิลสิ้นสุด"), true);
    }

    private void placeTarget(ClientPlayerEntity p, double dist) {
        Vec3d look = p.getRotationVec(1.0f);
        double l = Math.hypot(look.x, look.z);
        double fx = l < 1e-4 ? 0 : look.x / l, fz = l < 1e-4 ? 1 : look.z / l;
        double x = p.getX() + fx * dist, z = p.getZ() + fz * dist;
        targetRing  = new Ring(TEX_RING,  x, anchorY + 0.05, z, BIG_RING,   120f, 1_000_000);
        targetOuter = new Ring(TEX_OUTER, x, anchorY + 0.06, z, OUTER_RING, -70f, 1_000_000);
        rings.add(targetRing);
        rings.add(targetOuter);
    }

    private void spawnPair(double x, double y, double z, float inner, float outer, int life) {
        rings.add(new Ring(TEX_RING,  x, y,        z, inner, 140f, life));
        rings.add(new Ring(TEX_OUTER, x, y + 0.01, z, outer, -90f, life));
    }

    // ================= สกิล 2: หยินหยางสายฟ้าเพลิง (กด H) =================
    private static Ring upright(Identifier tex, float aspect, double x, double y, double z, float height, int life) {
        Ring r = new Ring(tex, true, x, y, z, height * aspect * 0.5f, height, 0f, life);
        rings.add(r);
        return r;
    }

    private static double groundY(MinecraftClient mc, double x, double y, double z) {
        BlockPos.Mutable bp = new BlockPos.Mutable();
        for (int i = 0; i < 10; i++) {
            bp.set(x, y + 3 - i, z);
            if (!mc.world.getBlockState(bp).isAir()) return bp.getY() + 1.0;
        }
        return y;
    }

    private void tickSkill2(MinecraftClient mc, ClientPlayerEntity p) {
        if (s2Cool > 0) s2Cool--;

        while (skill2Key.wasPressed()) {
            if (s2Active) continue;
            if (s2Cool > 0) { p.sendMessage(Text.literal("§7สกิลยังคูลดาวน์"), true); continue; }
            startSkill2(mc, p);
        }
        if (!s2Active) return;

        s2Ticks++;
        final int t = s2Ticks;
        final double ox = s2Origin.x, oy = s2Origin.y, oz = s2Origin.z;

        // ---- ช่วงชาร์จ: ก้อนไฟลอยขึ้นรอบตัว ----
        if (t < S2_CHARGE && t % 2 == 0) {
            double ang = rnd.nextDouble() * Math.PI * 2, rad = 0.8 + rnd.nextDouble() * 1.8;
            upright(TEX_FIRE, FIRE_ASPECT, ox + Math.cos(ang) * rad, oy, oz + Math.sin(ang) * rad,
                    1.4f + rnd.nextFloat(), 16).rise(0.06f).scale(0.5f, 1.3f);
        }

        // ---- ปล่อยพลัง: แฟลร์ระเบิดที่ตัว ----
        if (t == S2_CHARGE) {
            upright(TEX_FLARE, FLARE_ASPECT, ox, oy, oz, 4.5f, 12).tint(1f, 0.75f, 0.35f);
            mc.world.playSound(ox, oy, oz, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.6f, 1.4f, false);
        }

        // ---- สายฟ้าฟาดเป็นแนวไปข้างหน้า ----
        int rel = t - S2_CHARGE;
        if (rel >= 0 && rel < S2_STRIKES * 3 && rel % 3 == 0) {
            int i = rel / 3;
            double dist = 3.5 + i * 1.2;
            double lat = (i % 2 == 0 ? 1 : -1) * (0.6 + rnd.nextDouble() * 1.6);
            double px = -s2Dir.z, pz = s2Dir.x;
            double x = ox + s2Dir.x * dist + px * lat;
            double z = oz + s2Dir.z * dist + pz * lat;
            double gy = groundY(mc, x, oy, z);

            upright(TEX_BOLT, BOLT_ASPECT, x, gy, z, BOLT_HEIGHT, 7).tint(0.7f, 0.85f, 1f).flicker();
            upright(TEX_FLARE, FLARE_ASPECT, x, gy, z, 3.6f, 10).tint(1f, 0.7f, 0.3f);
            upright(TEX_FIRE, FIRE_ASPECT, x, gy, z, 2.6f, 18).rise(0.05f).scale(0.6f, 1.5f);
            rings.add(new Ring(TEX_RING, x, gy + 0.05, z, 1.8f, 160f, 14).tint(1f, 0.6f, 0.25f));
            rings.add(new Ring(TEX_TAIJI, x, gy + 0.06, z, 1.1f, 220f, 12).tint(1f, 0.85f, 0.4f));

            mc.world.playSound(x, gy, z, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.WEATHER, 0.5f, 1.2f + rnd.nextFloat() * 0.4f, false);
            mc.world.playSound(x, gy, z, SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT, SoundCategory.WEATHER, 0.6f, 1.0f, false);
        }

        if (t > S2_CHARGE + S2_STRIKES * 3 + 14) {
            s2Active = false;
            s2Cool = S2_COOLDOWN;
        }
    }

    private void startSkill2(MinecraftClient mc, ClientPlayerEntity p) {
        s2Active = true;
        s2Ticks = 0;
        s2Origin = new Vec3d(p.getX(), groundY(mc, p.getX(), p.getY(), p.getZ()), p.getZ());

        Vec3d look = p.getRotationVec(1.0f);
        double l = Math.hypot(look.x, look.z);
        s2Dir = l < 1e-4 ? new Vec3d(0, 0, 1) : new Vec3d(look.x / l, 0, look.z / l);

        int life = S2_CHARGE + S2_STRIKES * 3 + 14;
        rings.add(new Ring(TEX_TAIJI, s2Origin.x, s2Origin.y + 0.05, s2Origin.z, 2.6f, 120f, life).tint(1f, 0.85f, 0.4f));
        rings.add(new Ring(TEX_RING,  s2Origin.x, s2Origin.y + 0.06, s2Origin.z, 3.6f, -80f, life).tint(1f, 0.5f, 0.2f));

        p.playSound(SoundEvents.ENTITY_BLAZE_SHOOT, 1.0f, 0.8f);
        p.sendMessage(Text.literal("§6陰陽 §eหยินหยางสายฟ้าเพลิง"), true);
    }

    // ================= เรนเดอร์ =================
    private void onRender(WorldRenderContext ctx) {
        if (rings.isEmpty()) return;
        Vec3d cam = ctx.camera().getPos();
        float camYaw = ctx.camera().getYaw();
        float td = ctx.tickDelta();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE); // additive
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);

        BufferBuilder bb = Tessellator.getInstance().getBuffer();

        for (Ring r : rings) {
            float age = r.age + td;

            float in  = Math.min(1f, age / 4f);
            float out = Math.min(1f, (r.max - age) / 10f);
            float a = Math.max(0f, Math.min(in, out));
            if (r.flicker && (r.age & 1) == 1) a *= 0.65f;
            if (a <= 0.01f) continue;

            float f = r.s0 + (r.s1 - r.s0) * Math.min(1f, age / r.max);

            RenderSystem.setShaderTexture(0, r.tex);

            MatrixStack ms = ctx.matrixStack();
            ms.push();
            ms.translate(r.x - cam.x, r.y + r.rise * age - cam.y, r.z - cam.z);
            Matrix4f m;

            bb.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            if (r.upright) {
                ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f - camYaw));
                m = ms.peek().getPositionMatrix();
                float hw = r.w * f, hh = r.h * f;
                bb.vertex(m, -hw, 0,  0).texture(0, 1).color(r.r, r.g, r.b, a).next();
                bb.vertex(m, -hw, hh, 0).texture(0, 0).color(r.r, r.g, r.b, a).next();
                bb.vertex(m,  hw, hh, 0).texture(1, 0).color(r.r, r.g, r.b, a).next();
                bb.vertex(m,  hw, 0,  0).texture(1, 1).color(r.r, r.g, r.b, a).next();
            } else {
                ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(r.spin * age * 0.05f));
                m = ms.peek().getPositionMatrix();
                float s = r.w * f * Math.min(1f, 0.4f + age / 5f);
                bb.vertex(m, -s, 0, -s).texture(0, 0).color(r.r, r.g, r.b, a).next();
                bb.vertex(m, -s, 0,  s).texture(0, 1).color(r.r, r.g, r.b, a).next();
                bb.vertex(m,  s, 0,  s).texture(1, 1).color(r.r, r.g, r.b, a).next();
                bb.vertex(m,  s, 0, -s).texture(1, 0).color(r.r, r.g, r.b, a).next();
            }
            BufferRenderer.drawWithGlobalProgram(bb.end());
            ms.pop();
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }
}
