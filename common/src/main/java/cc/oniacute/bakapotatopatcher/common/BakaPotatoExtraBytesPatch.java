package cc.oniacute.bakapotatopatcher.common;

public final class BakaPotatoExtraBytesPatch {
    private BakaPotatoExtraBytesPatch() {
    }

    public static boolean isActive(BakaPotatoClientConfig config) {
        return config != null
                && config.extraBytesPatch != null
                && config.extraBytesPatch.enabled
                && BakaPotatoPatchApplicability.isEnabledForCurrentServer(config);
    }

    public static String mode(BakaPotatoClientConfig config) {
        if (config == null || config.extraBytesPatch == null) {
            return BakaPotatoClientConfig.ExtraBytesPatchConfig.MODE_DISCONNECT;
        }
        config.normalize();
        return config.extraBytesPatch.mode;
    }
}
