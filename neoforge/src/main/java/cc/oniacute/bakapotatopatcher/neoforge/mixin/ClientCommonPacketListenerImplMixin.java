package cc.oniacute.bakapotatopatcher.neoforge.mixin;

import cc.oniacute.bakapotatopatcher.common.BakaPotatoClientConfig;
import cc.oniacute.bakapotatopatcher.common.BakaPotatoClientConfigManager;
import cc.oniacute.bakapotatopatcher.common.BakaPotatoDebugInfo;
import cc.oniacute.bakapotatopatcher.common.BakaPotatoPatchApplicability;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.URI;
import java.net.URL;

@Mixin(ClientCommonPacketListenerImpl.class)
public abstract class ClientCommonPacketListenerImplMixin {
    @Shadow
    @Final
    protected Minecraft minecraft;

    @Shadow
    @Final
    protected ServerData serverData;

    @Inject(
            method = "handleResourcePackPush",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/common/ClientboundResourcePackPushPacket;id()Ljava/util/UUID;"),
            cancellable = true
    )
    private void bakapotatopatcher$autoAcceptServerResourcePack(ClientboundResourcePackPushPacket packet, CallbackInfo ci) {
        BakaPotatoClientConfig config = BakaPotatoClientConfigManager.get();
        if (serverData != null) {
            BakaPotatoPatchApplicability.setCurrentServerAddress(serverData.ip);
        }
        if (!config.resourcePacks.autoAcceptServerPacks || !BakaPotatoPatchApplicability.isEnabledForCurrentServer(config)) {
            return;
        }
        try {
            URL url = URI.create(packet.url()).toURL();
            if (serverData != null) {
                serverData.setResourcePackStatus(ServerData.ServerPackStatus.ENABLED);
            }
            minecraft.getDownloadedPackSource().allowServerPacks();
            minecraft.getDownloadedPackSource().pushPack(packet.id(), url, packet.hash());
            BakaPotatoDebugInfo.log("auto accepted server resource pack");
            ci.cancel();
        } catch (Exception exception) {
            // Let vanilla report invalid resource pack URLs to the server.
        }
    }
}
