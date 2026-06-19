package cc.oniacute.bakapotatopatcher.forge;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ForgeWaypointPayload(Type<ForgeWaypointPayload> type, byte[] data) implements CustomPacketPayload {
    public static StreamCodec<RegistryFriendlyByteBuf, ForgeWaypointPayload> codec(Type<ForgeWaypointPayload> type) {
        return StreamCodec.of(
                (buffer, payload) -> buffer.writeBytes(payload.data()),
                buffer -> {
                    byte[] data = new byte[buffer.readableBytes()];
                    buffer.readBytes(data);
                    return new ForgeWaypointPayload(type, data);
                }
        );
    }
}
