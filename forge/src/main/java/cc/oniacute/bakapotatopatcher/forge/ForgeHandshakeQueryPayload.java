package cc.oniacute.bakapotatopatcher.forge;

import cc.oniacute.bakapotatopatcher.common.BakaPotatoProtocol;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ForgeHandshakeQueryPayload(byte[] data) implements CustomPacketPayload {
    public static final Type<ForgeHandshakeQueryPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(BakaPotatoProtocol.CHANNEL_NAMESPACE, BakaPotatoProtocol.CHANNEL_PATH)
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, ForgeHandshakeQueryPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> buffer.writeBytes(payload.data()),
            buffer -> {
                byte[] data = new byte[buffer.readableBytes()];
                buffer.readBytes(data);
                return new ForgeHandshakeQueryPayload(data);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
