package cc.oniacute.bakapotatopatcher.neoforge;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record NeoForgeWaypointPayload(Type<NeoForgeWaypointPayload> type, byte[] data) implements CustomPacketPayload {
    public static StreamCodec<RegistryFriendlyByteBuf, NeoForgeWaypointPayload> codec(Type<NeoForgeWaypointPayload> type) {
        return StreamCodec.of(
                (buffer, payload) -> buffer.writeBytes(payload.data()),
                buffer -> {
                    byte[] data = new byte[buffer.readableBytes()];
                    buffer.readBytes(data);
                    return new NeoForgeWaypointPayload(type, data);
                }
        );
    }
}
