package cc.oniacute.bakapotatopatcher.common;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

public final class BakaPotatoClientTelemetry {
    public static final long STATS_INTERVAL_TICKS = 20L * 60L * 3L;

    private static final AtomicLong packetsSentToServer = new AtomicLong();
    private static long nextStatsTick = ThreadLocalRandom.current().nextLong(20L * 15L, STATS_INTERVAL_TICKS);
    private static String lastServerAddress = "";

    private BakaPotatoClientTelemetry() {
    }

    public static void markPacketSentToServer() {
        packetsSentToServer.incrementAndGet();
    }

    public static long packetsSentToServer() {
        return packetsSentToServer.get();
    }

    public static boolean shouldSendStats(long clientTick, String serverAddress) {
        if (serverAddress == null || serverAddress.isBlank()) {
            lastServerAddress = "";
            return false;
        }
        if (!serverAddress.equals(lastServerAddress)) {
            lastServerAddress = serverAddress;
            nextStatsTick = clientTick + ThreadLocalRandom.current().nextLong(20L * 15L, 20L * 45L);
        }
        return clientTick >= nextStatsTick;
    }

    public static ClientStatsPacket createStats(String serverAddress) {
        nextStatsTick += STATS_INTERVAL_TICKS + ThreadLocalRandom.current().nextLong(0L, 20L * 30L);
        return new ClientStatsPacket(serverAddress, packetsSentToServer());
    }
}
