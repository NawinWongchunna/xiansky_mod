package dev.ascension.rings.mixin;

import dev.ascension.rings.client.ClientState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** ท้องฟ้า/เมฆ เปลี่ยนเป็นสีแดงเข้มตามความเข้มของสกิล */
@Mixin(ClientWorld.class)
public abstract class ClientWorldMixin {
    @Unique
    private static final Vec3d ASCENSION_SKY = new Vec3d(0.32, 0.0, 0.03);

    @Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true)
    private void ascension$sky(Vec3d cameraPos, float tickDelta, CallbackInfoReturnable<Vec3d> cir) {
        float k = ClientState.skyIntensity(tickDelta);
        if (k <= 0f) return;
        cir.setReturnValue(cir.getReturnValue().multiply(1.0 - k).add(ASCENSION_SKY.multiply(k)));
    }

    @Inject(method = "getCloudsColor", at = @At("RETURN"), cancellable = true)
    private void ascension$clouds(float tickDelta, CallbackInfoReturnable<Vec3d> cir) {
        float k = ClientState.skyIntensity(tickDelta);
        if (k <= 0f) return;
        cir.setReturnValue(cir.getReturnValue().multiply(1.0 - k * 0.8).add(ASCENSION_SKY.multiply(k * 1.5)));
    }
}
