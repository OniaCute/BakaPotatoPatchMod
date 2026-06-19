package cc.oniacute.bakapotatopatcher.neoforge;

import cc.oniacute.bakapotatopatcher.common.BakaPotatoProtocol;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record NeoForgeHandshakeResponsePayload(byte[] data) implements CustomPacketPayload {
    public static final Type<NeoForgeHandshakeResponsePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(BakaPotatoProtocol.CHANNEL_NAMESPACE, BakaPotatoProtocol.CHANNEL_PATH)
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, NeoForgeHandshakeResponsePayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> buffer.writeBytes(payload.data()),
            buffer -> {
                byte[] data = new byte[buffer.readableBytes()];
                buffer.readBytes(data);
                return new NeoForgeHandshakeResponsePayload(data);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
