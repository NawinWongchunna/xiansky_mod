package com.ascension.rings.mixin;

import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Lets the ability reset vanilla's "Flying is not enabled on this server" kick counter
 * while the player is hovering on a dedicated server.
 */
@Mixin(ServerPlayNetworkHandler.class)
public interface ServerPlayNetworkHandlerAccessor {
    @Accessor("floatingTicks")
    void ascension$setFloatingTicks(int ticks);

    @Accessor("vehicleFloatingTicks")
    void ascension$setVehicleFloatingTicks(int ticks);
}
