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
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class XianSkyClient implements ClientModInitializer {

    // ===== ตั้งค่า (ปรับได้เลย) =====
    static final int    DURATION     = 300;   // ติก (300 = 15 วิ)
    static final double RING_DIST    = 5.0;   // วงแหวนอยู่ห่างหน้าตัวกี่บล็อก
    static final double RING_RADIUS  = 1.4;   // เหยียบใกล้แค่ไหนถึงพุ่ง
    static final int    DASH_TICKS   = 14;
    static final double DASH_SPEED   = 1.1;   // บล็อก/ติก
    static final float  BIG_RING     = 1.6f;  // ครึ่งหนึ่งของขนาดวงแหวนเป้าหมาย (บล็อก)
    static final float  OUTER_RING   = 2.6f;  // วงนอก (เท็กเจอร์ 103)

    static final Identifier TEX_RING  = new Identifier("xiansky", "textures/vfx/ring.png");
    static final Identifier TEX_OUTER = new Identifier("xiansky", "textures/vfx/outer.png");

    // ===== สถานะสกิล =====
    static boolean active = false;
    static double anchorY;
    static int ticks, dash;
    static Vec3d dashDir = Vec3d.ZERO;
    static Ring targetRing, targetOuter;

    static final List<Ring> rings = new ArrayList<>();
    static KeyBinding skillKey;

    static class Ring {
        final Identifier tex;
        final double x, y, z;
        final float half, spin;
        int age = 0, max;
        Ring(Identifier tex, double x, double y, double z, float half, float spin, int max) {
            this.tex = tex; this.x = x; this.y = y; this.z = z;
            this.half = half; this.spin = spin; this.max = max;
        }
    }

    @Override
    public void onInitializeClient() {
        skillKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.xiansky.skill", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, "key.categories.xiansky"));

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
        WorldRenderEvents.AFTER_TRANSLUCENT.register(this::onRender);
    }

    // ================= ลอจิก =================
    private void onTick(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null) { active = false; rings.clear(); return; }

        // อัปเดตอายุวงแหวนทุกติก
        for (Iterator<Ring> it = rings.iterator(); it.hasNext(); ) {
            Ring r = it.next();
            if (++r.age >= r.max) it.remove();
        }

        while (skillKey.wasPressed()) {
            if (active) stop(p); else start(p);
        }
        if (!active) return;

        ticks++;
        if (ticks > DURATION || (p.isSneaking() && ticks > 10)) { stop(p); return; }

        // ----- พุ่ง -----
        if (dash > 0) {
            p.setVelocity(dashDir.x * DASH_SPEED, 0, dashDir.z * DASH_SPEED);
            if (dash % 2 == 0) spawnPair(p.getX(), anchorY + 0.05, p.getZ(), 0.9f, 1.8f, 22);
            dash--;
            if (p.horizontalCollision) dash = 0;
            if (dash == 0) placeTarget(p, RING_DIST + 2);
        }

        // ----- ล็อกความสูง = เดินบนอากาศ -----
        if (p.getY() < anchorY) {
            p.setPosition(p.getX(), anchorY, p.getZ());
            Vec3d v = p.getVelocity();
            if (v.y < 0) p.setVelocity(v.x, 0, v.z);
        }
        if (p.getY() <= anchorY + 0.01) p.setOnGround(true);
        p.fallDistance = 0;

        // ----- รอยเท้าวงแหวน -----
        if (dash == 0 && ticks % 8 == 0 && p.getVelocity().horizontalLengthSquared() > 0.0004) {
            rings.add(new Ring(TEX_RING, p.getX(), anchorY + 0.03, p.getZ(), 0.8f, 100f, 20));
        }

        // ----- เหยียบวงแหวน -> พุ่ง -----
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

    // ================= เรนเดอร์ =================
    private void onRender(WorldRenderContext ctx) {
        if (rings.isEmpty()) return;
        Vec3d cam = ctx.camera().getPos();
        float td = ctx.tickDelta();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE); // additive
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);

        Identifier last = null;
        BufferBuilder bb = Tessellator.getInstance().getBuffer();

        for (Ring r : rings) {
            float age = r.age + td;

            float in  = Math.min(1f, age / 4f);
            float out = Math.min(1f, (r.max - age) / 10f);
            float a = Math.max(0f, Math.min(in, out));
            if (a <= 0.01f) continue;
            float s = r.half * Math.min(1f, 0.4f + age / 5f);

            RenderSystem.setShaderTexture(0, r.tex);

            MatrixStack ms = ctx.matrixStack();
            ms.push();
            ms.translate(r.x - cam.x, r.y - cam.y, r.z - cam.z);
            ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(r.spin * age * 0.05f * 20f / 20f));
            Matrix4f m = ms.peek().getPositionMatrix();

            bb.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            bb.vertex(m, -s, 0, -s).texture(0, 0).color(0.75f, 0.95f, 1f, a).next();
            bb.vertex(m, -s, 0,  s).texture(0, 1).color(0.75f, 0.95f, 1f, a).next();
            bb.vertex(m,  s, 0,  s).texture(1, 1).color(0.75f, 0.95f, 1f, a).next();
            bb.vertex(m,  s, 0, -s).texture(1, 0).color(0.75f, 0.95f, 1f, a).next();
            BufferRenderer.drawWithGlobalProgram(bb.end());
            ms.pop();
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }
}
