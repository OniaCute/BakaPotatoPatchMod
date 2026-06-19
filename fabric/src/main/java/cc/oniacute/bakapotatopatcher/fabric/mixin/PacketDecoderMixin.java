package cc.oniacute.bakapotatopatcher.fabric.mixin;

import cc.oniacute.bakapotatopatcher.common.BakaPotatoClientConfig;
import cc.oniacute.bakapotatopatcher.common.BakaPotatoClientConfigManager;
import cc.oniacute.bakapotatopatcher.common.BakaPotatoDebugInfo;
import cc.oniacute.bakapotatopatcher.common.BakaPotatoExtraBytesPatch;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.PacketDecoder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(PacketDecoder.class)
public abstract class PacketDecoderMixin {
    @Unique
    private boolean bakapotatopatcher$ignoreDecodedPacket;

    @Redirect(
            method = "decode",
            at = @At(value = "INVOKE", target = "Lio/netty/buffer/ByteBuf;readableBytes()I", ordinal = 2)
    )
    private int bakapotatopatcher$handleExtraBytes(ByteBuf buffer) {
        int extraBytes = buffer.readableBytes();
        if (extraBytes <= 0) {
            return extraBytes;
        }
        BakaPotatoClientConfig config = BakaPotatoClientConfigManager.get();
        if (!BakaPotatoExtraBytesPatch.isActive(config)) {
            return extraBytes;
        }
        String mode = BakaPotatoExtraBytesPatch.mode(config);
        if (BakaPotatoClientConfig.ExtraBytesPatchConfig.MODE_DISCONNECT.equals(mode)) {
            return extraBytes;
        }
        buffer.skipBytes(extraBytes);
        bakapotatopatcher$ignoreDecodedPacket = BakaPotatoClientConfig.ExtraBytesPatchConfig.MODE_IGNORE.equals(mode);
        BakaPotatoDebugInfo.log("handled packet with " + extraBytes + " extra bytes: " + mode);
        return 0;
    }

    @Redirect(
            method = "decode",
            at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z")
    )
    private boolean bakapotatopatcher$maybeDropDecodedPacket(List<Object> packets, Object packet) {
        if (bakapotatopatcher$ignoreDecodedPacket) {
            bakapotatopatcher$ignoreDecodedPacket = false;
            return false;
        }
        return packets.add(packet);
    }
}
