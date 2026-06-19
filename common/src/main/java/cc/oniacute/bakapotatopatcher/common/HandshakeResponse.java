package cc.oniacute.bakapotatopatcher.common;

import java.util.List;

public record HandshakeResponse(
        int protocolVersion,
        String modVersion,
        String loader,
        String minecraftVersion,
        List<String> capabilities,
        String serverAddress,
        List<String> modList,
        String hardwareIdHash
) {
    public HandshakeResponse {
        capabilities = List.copyOf(capabilities);
        serverAddress = serverAddress == null ? "" : serverAddress;
        modList = List.copyOf(modList);
        hardwareIdHash = hardwareIdHash == null ? "" : hardwareIdHash;
    }
}
