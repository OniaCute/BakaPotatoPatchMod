package cc.oniacute.bakapotatopatcher.common;

import java.util.List;

public final class ClientHandshakeFactory {
    private ClientHandshakeFactory() {
    }

    public static byte[] responsePayload(String modVersion, LoaderType loaderType, String minecraftVersion) {
        HandshakeResponse response = new HandshakeResponse(
                BakaPotatoProtocol.PROTOCOL_VERSION,
                modVersion,
                loaderType.id(),
                minecraftVersion,
                List.of("handshake", "client_info_query", "client_stats"),
                BakaPotatoPatchApplicability.currentServer(),
                List.of(),
                ""
        );
        return BakaPotatoProtocol.encodeResponse(response);
    }

    public static byte[] responsePayload(String modVersion, LoaderType loaderType, String minecraftVersion, List<String> modList) {
        return responsePayload(modVersion, loaderType, minecraftVersion);
    }
}
