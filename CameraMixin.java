package dev.ascension.rings.mixin;

import dev.ascension.rings.client.ClientState;
import dev.ascension.rings.client.camera.CameraEffects;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import net.minecraft.world.RaycastContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** กล้อง cutscene: orbit + ถอย + เงย + สั่น */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @Shadow
    protected abstract void setPos(Vec3d pos);

    @Shadow
    protected abstract void moveBy(double x, double y, double z);

    @Shadow
    public abstract float getYaw();

    @Shadow
    public abstract float getPitch();

    @Shadow
    public abstract Vec3d getPos();

    @Inject(method = "update", at = @At("TAIL"))
    private void ascension$cutscene(BlockView area, Entity focused, boolean thirdPerson, boolean inverseView,
                                    float tickDelta, CallbackInfo ci) {
        float t = ClientState.cinematicTime(tickDelta);
        if (t < 0 || focused == null) return;

        // เริ่มจากตำแหน่งตา (ไม่ใช่ตำแหน่ง third-person ปกติ) แล้ว orbit เอง
        Vec3d eye = focused.getLerpedPos(tickDelta).add(0, focused.getEyeHeight(focused.getPose()), 0);
        float baseYaw = getYaw();
        float basePitch = getPitch();

        setPos(eye);
        setRotation(baseYaw + CameraEffects.yaw(t) + CameraEffects.shakeYaw(t),
                basePitch + CameraEffects.pitch(t) + CameraEffects.shakePitch(t));
        moveBy(-(4.0 + CameraEffects.distance(t)), CameraEffects.lower(t), 0);

        // กันกล้องทะลุบล็อก
        Vec3d target = getPos();
        HitResult hit = focused.getWorld().raycast(new RaycastContext(eye, target,
                RaycastContext.ShapeType.VISUAL, RaycastContext.FluidHandling.NONE, focused));
        if (hit.getType() != HitResult.Type.MISS) {
            setPos(eye.add(hit.getPos().subtract(eye).multiply(0.9)));
        }
    }
}
