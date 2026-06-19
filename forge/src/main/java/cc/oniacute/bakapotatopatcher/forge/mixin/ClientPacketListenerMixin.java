package cc.oniacute.bakapotatopatcher.forge.mixin;

import cc.oniacute.bakapotatopatcher.common.BakaPotatoClientConfigManager;
import cc.oniacute.bakapotatopatcher.common.BakaPotatoDebugInfo;
import cc.oniacute.bakapotatopatcher.common.BakaPotatoWaypointGuard;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundTrackedWaypointPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @Inject(method = "handleWaypoint", at = @At("HEAD"), cancellable = true)
    private void bakapotatopatcher$dropNativeWaypoint(ClientboundTrackedWaypointPacket packet, CallbackInfo ci) {
        if (BakaPotatoWaypointGuard.shouldDropNativeWaypoint(BakaPotatoClientConfigManager.get())) {
            if (BakaPotatoClientConfigManager.get().waypoint.debugLogDroppedPackets) {
                System.out.println("[BakaPotatoPatcher] Dropped native minecraft:waypoint packet.");
            }
            BakaPotatoDebugInfo.log("dropped native minecraft:waypoint packet");
            ci.cancel();
        }
    }
}
