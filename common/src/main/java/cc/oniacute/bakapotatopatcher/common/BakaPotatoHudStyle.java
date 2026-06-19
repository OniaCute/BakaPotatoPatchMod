package cc.oniacute.bakapotatopatcher.common;

public final class BakaPotatoHudStyle {
    private BakaPotatoHudStyle() {
    }

    public static int textColor(BakaPotatoClientConfig config) {
        return 0xFF000000 | rgb(config == null || config.debug == null ? "#FFFFFF" : config.debug.hudTextColor);
    }

    public static int backgroundColor(BakaPotatoClientConfig config) {
        if (config == null || config.debug == null || !config.debug.hudBackgroundEnabled) {
            return 0;
        }
        int alpha = Math.max(0, Math.min(255, config.debug.hudBackgroundAlpha));
        return (alpha << 24) | rgb(config.debug.hudBackgroundColor);
    }

    private static int rgb(String value) {
        String normalized = value == null ? "FFFFFF" : value.trim();
        if (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        }
        try {
            return Integer.parseInt(normalized, 16) & 0xFFFFFF;
        } catch (NumberFormatException exception) {
            return 0xFFFFFF;
        }
    }
}
