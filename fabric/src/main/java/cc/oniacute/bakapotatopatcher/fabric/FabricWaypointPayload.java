package cc.oniacute.bakapotatopatcher.fabric;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record FabricWaypointPayload(Type<FabricWaypointPayload> type, byte[] data) implements CustomPacketPayload {
    public static StreamCodec<RegistryFriendlyByteBuf, FabricWaypointPayload> codec(Type<FabricWaypointPayload> type) {
        return StreamCodec.of(
                (buffer, payload) -> buffer.writeBytes(payload.data()),
                buffer -> {
                    byte[] data = new byte[buffer.readableBytes()];
                    buffer.readBytes(data);
                    return new FabricWaypointPayload(type, data);
                }
        );
    }
}
