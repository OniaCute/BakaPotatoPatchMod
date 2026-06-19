package cc.oniacute.bakapotatopatcher.common;

import java.util.Locale;

public final class BakaPotatoWaypointGuard {
    public static final String WAYPOINT_CHANNEL_NAMESPACE = BakaPotatoProtocol.MOD_ID;
    public static final String WAYPOINT_CHANNEL_PATH = "waypoint";
    public static final String WAYPOINT_CHANNEL = WAYPOINT_CHANNEL_NAMESPACE + ":" + WAYPOINT_CHANNEL_PATH;

    private static final byte[] WAYPOINT_MAGIC = new byte[] {'B', 'P', 'P', 'W'};

    private BakaPotatoWaypointGuard() {
    }

    public static boolean shouldDrop(byte[] payload, BakaPotatoClientConfig config) {
        if (config == null || config.waypoint == null || !config.waypoint.enabled
                || !BakaPotatoPatchApplicability.isEnabledForCurrentServer(config)) {
            return false;
        }
        if (payload == null || payload.length == 0 || isAllZero(payload)) {
            return true;
        }
        return config.waypoint.dropInvalidPackets && !hasWaypointMagic(payload);
    }

    public static boolean shouldDropNativeWaypoint(BakaPotatoClientConfig config) {
        return config != null
                && config.waypoint != null
                && config.waypoint.enabled
                && config.waypoint.dropInvalidPackets
                && BakaPotatoPatchApplicability.isEnabledForCurrentServer(config);
    }

    public static boolean isBuiltinProfileEnabled(BakaPotatoClientConfig config, String profile) {
        return config != null
                && config.waypoint != null
                && config.waypoint.builtinDetectionProfiles.stream()
                .anyMatch(value -> value.equals(profile.toLowerCase(Locale.ROOT)));
    }

    private static boolean isAllZero(byte[] payload) {
        for (byte value : payload) {
            if (value != 0) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasWaypointMagic(byte[] payload) {
        if (payload.length < WAYPOINT_MAGIC.length) {
            return false;
        }
        for (int index = 0; index < WAYPOINT_MAGIC.length; index++) {
            if (payload[index] != WAYPOINT_MAGIC[index]) {
                return false;
            }
        }
        return true;
    }
}
