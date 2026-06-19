package cc.oniacute.bakapotatopatcher.common;

public record ClientStatsPacket(
        String serverAddress,
        long packetsSentToServer
) {
    public ClientStatsPacket {
        serverAddress = serverAddress == null ? "" : serverAddress;
    }
}
