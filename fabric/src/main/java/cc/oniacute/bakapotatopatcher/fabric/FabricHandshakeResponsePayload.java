package cc.oniacute.bakapotatopatcher.fabric;

import cc.oniacute.bakapotatopatcher.common.BakaPotatoProtocol;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record FabricHandshakeResponsePayload(byte[] data) implements CustomPacketPayload {
    public static final Type<FabricHandshakeResponsePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(BakaPotatoProtocol.CHANNEL_NAMESPACE, BakaPotatoProtocol.CHANNEL_PATH)
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, FabricHandshakeResponsePayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> buffer.writeBytes(payload.data()),
            buffer -> {
                byte[] data = new byte[buffer.readableBytes()];
                buffer.readBytes(data);
                return new FabricHandshakeResponsePayload(data);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
