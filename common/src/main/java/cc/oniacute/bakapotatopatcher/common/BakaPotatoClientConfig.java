package cc.oniacute.bakapotatopatcher.common;

import java.util.ArrayList;
import java.util.List;

public final class BakaPotatoClientConfig {
    public UiConfig ui = new UiConfig();
    public PatchConfig patches = new PatchConfig();
    public WaypointConfig waypoint = new WaypointConfig();
    public ExtraBytesPatchConfig extraBytesPatch = new ExtraBytesPatchConfig();
    public ResourcePackConfig resourcePacks = new ResourcePackConfig();
    public PrivacyConfig privacy = new PrivacyConfig();
    public DebugConfig debug = new DebugConfig();

    public BakaPotatoClientConfig copy() {
        BakaPotatoClientConfig copy = new BakaPotatoClientConfig();
        copy.ui.demoBoolean = ui.demoBoolean;
        copy.ui.demoString = ui.demoString;
        copy.ui.demoInteger = ui.demoInteger;
        copy.ui.demoDecimal = ui.demoDecimal;
        copy.ui.demoSingleChoice = ui.demoSingleChoice;
        copy.ui.demoMultiChoice = new ArrayList<>(ui.demoMultiChoice);
        copy.patches.applyMode = patches.applyMode;
        copy.patches.enableForAllServers = patches.enableForAllServers;
        copy.patches.remoteDomainListUrl = patches.remoteDomainListUrl;
        copy.waypoint.enabled = waypoint.enabled;
        copy.waypoint.builtinDetectionProfiles = new ArrayList<>(waypoint.builtinDetectionProfiles);
        copy.waypoint.customChannels = new ArrayList<>(waypoint.customChannels);
        copy.waypoint.dropInvalidPackets = waypoint.dropInvalidPackets;
        copy.waypoint.debugLogDroppedPackets = waypoint.debugLogDroppedPackets;
        copy.extraBytesPatch.enabled = extraBytesPatch.enabled;
        copy.extraBytesPatch.mode = extraBytesPatch.mode;
        copy.resourcePacks.autoAcceptServerPacks = resourcePacks.autoAcceptServerPacks;
        copy.privacy.sendModList = privacy.sendModList;
        copy.privacy.modListMode = privacy.modListMode;
        copy.debug.showHud = debug.showHud;
        copy.debug.hudBackgroundEnabled = debug.hudBackgroundEnabled;
        copy.debug.hudBackgroundColor = debug.hudBackgroundColor;
        copy.debug.hudBackgroundAlpha = debug.hudBackgroundAlpha;
        copy.debug.hudTextColor = debug.hudTextColor;
        copy.debug.hudStaySeconds = debug.hudStaySeconds;
        copy.debug.hudMaxLines = debug.hudMaxLines;
        return copy;
    }

    public void normalize() {
        if (ui == null) {
            ui = new UiConfig();
        }
        if (waypoint == null) {
            waypoint = new WaypointConfig();
        }
        if (extraBytesPatch == null) {
            extraBytesPatch = new ExtraBytesPatchConfig();
        }
        if (patches == null) {
            patches = new PatchConfig();
        }
        if (resourcePacks == null) {
            resourcePacks = new ResourcePackConfig();
        }
        if (privacy == null) {
            privacy = new PrivacyConfig();
        }
        if (debug == null) {
            debug = new DebugConfig();
        }
        if (ui.demoString == null) {
            ui.demoString = "";
        }
        if (ui.demoSingleChoice == null || !UiConfig.SINGLE_CHOICES.contains(ui.demoSingleChoice)) {
            ui.demoSingleChoice = UiConfig.SINGLE_CHOICES.get(0);
        }
        if (ui.demoMultiChoice == null) {
            ui.demoMultiChoice = new ArrayList<>();
        }
        ui.demoMultiChoice.removeIf(value -> !UiConfig.MULTI_CHOICES.contains(value));
        if (waypoint.builtinDetectionProfiles == null) {
            waypoint.builtinDetectionProfiles = new ArrayList<>();
        }
        if (waypoint.builtinDetectionProfiles.isEmpty()) {
            waypoint.builtinDetectionProfiles.add("bakapotato");
            waypoint.builtinDetectionProfiles.add("custom");
        }
        waypoint.builtinDetectionProfiles.removeIf(value -> !"bakapotato".equals(value) && !"custom".equals(value));
        if (waypoint.customChannels == null) {
            waypoint.customChannels = new ArrayList<>();
        }
        waypoint.customChannels.replaceAll(String::trim);
        waypoint.customChannels.removeIf(String::isEmpty);
        if (!ExtraBytesPatchConfig.MODE_FORCE_PARSE.equals(extraBytesPatch.mode)
                && !ExtraBytesPatchConfig.MODE_IGNORE.equals(extraBytesPatch.mode)
                && !ExtraBytesPatchConfig.MODE_DISCONNECT.equals(extraBytesPatch.mode)) {
            extraBytesPatch.mode = ExtraBytesPatchConfig.MODE_IGNORE;
        }
        if (patches.remoteDomainListUrl == null || patches.remoteDomainListUrl.isBlank()) {
            patches.remoteDomainListUrl = PatchConfig.DEFAULT_REMOTE_DOMAIN_LIST_URL;
        }
        if (!PatchConfig.APPLY_MODE_ALL_SERVERS.equals(patches.applyMode)
                && !PatchConfig.APPLY_MODE_BAKAPOTATO_SERVERS.equals(patches.applyMode)) {
            patches.applyMode = patches.enableForAllServers
                    ? PatchConfig.APPLY_MODE_ALL_SERVERS
                    : PatchConfig.APPLY_MODE_BAKAPOTATO_SERVERS;
        }
        patches.enableForAllServers = PatchConfig.APPLY_MODE_ALL_SERVERS.equals(patches.applyMode);
        debug.hudBackgroundColor = normalizeColor(debug.hudBackgroundColor, "#000000");
        debug.hudTextColor = normalizeColor(debug.hudTextColor, "#FFFFFF");
        debug.hudBackgroundAlpha = Math.max(0, Math.min(255, debug.hudBackgroundAlpha));
        debug.hudStaySeconds = Math.max(1, Math.min(120, debug.hudStaySeconds));
        debug.hudMaxLines = Math.max(1, Math.min(32, debug.hudMaxLines));
        if (privacy.modListMode == null || !PrivacyConfig.MOD_LIST_MODES.contains(privacy.modListMode)) {
            privacy.modListMode = PrivacyConfig.MOD_LIST_NECESSARY;
        }
    }

    private static String normalizeColor(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String normalized = value.trim();
        if (!normalized.startsWith("#")) {
            normalized = "#" + normalized;
        }
        return normalized.matches("#[0-9a-fA-F]{6}") ? normalized.toUpperCase() : fallback;
    }

    public static BakaPotatoClientConfig defaults() {
        BakaPotatoClientConfig config = new BakaPotatoClientConfig();
        config.normalize();
        return config;
    }

    public static final class UiConfig {
        public static final List<String> SINGLE_CHOICES = List.of("option_a", "option_b", "option_c");
        public static final List<String> MULTI_CHOICES = List.of("alpha", "beta", "gamma");

        public boolean demoBoolean = true;
        public String demoString = "BakaPotato";
        public int demoInteger = 16;
        public double demoDecimal = 1.5D;
        public String demoSingleChoice = SINGLE_CHOICES.get(0);
        public List<String> demoMultiChoice = new ArrayList<>(List.of("alpha", "gamma"));
    }

    public static final class WaypointConfig {
        public boolean enabled = true;
        public List<String> builtinDetectionProfiles = new ArrayList<>(List.of("bakapotato", "custom"));
        public List<String> customChannels = new ArrayList<>();
        public boolean dropInvalidPackets = true;
        public boolean debugLogDroppedPackets = false;
    }

    public static final class ExtraBytesPatchConfig {
        public static final String MODE_FORCE_PARSE = "force_parse";
        public static final String MODE_IGNORE = "ignore";
        public static final String MODE_DISCONNECT = "disconnect";
        public static final List<String> MODES = List.of(MODE_FORCE_PARSE, MODE_IGNORE, MODE_DISCONNECT);

        public boolean enabled = true;
        public String mode = MODE_IGNORE;
    }

    public static final class PatchConfig {
        public static final String DEFAULT_REMOTE_DOMAIN_LIST_URL = "https://assets.oniacute.cc/bakapotato/domians.onia";
        public static final String APPLY_MODE_BAKAPOTATO_SERVERS = "bakapotato_servers";
        public static final String APPLY_MODE_ALL_SERVERS = "all_servers";

        public String applyMode = APPLY_MODE_BAKAPOTATO_SERVERS;
        public boolean enableForAllServers = false;
        public String remoteDomainListUrl = DEFAULT_REMOTE_DOMAIN_LIST_URL;
    }

    public static final class ResourcePackConfig {
        public boolean autoAcceptServerPacks = false;
    }

    public static final class PrivacyConfig {
        public static final String MOD_LIST_ALL = "all";
        public static final String MOD_LIST_NECESSARY = "necessary";
        public static final List<String> MOD_LIST_MODES = List.of(MOD_LIST_ALL, MOD_LIST_NECESSARY);

        public boolean sendModList = true;
        public String modListMode = MOD_LIST_NECESSARY;
    }

    public static final class DebugConfig {
        public boolean showHud = false;
        public boolean hudBackgroundEnabled = true;
        public String hudBackgroundColor = "#000000";
        public int hudBackgroundAlpha = 96;
        public String hudTextColor = "#FFFFFF";
        public int hudStaySeconds = 8;
        public int hudMaxLines = 8;
    }
}
