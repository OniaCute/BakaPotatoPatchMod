package cc.oniacute.bakapotatopatcher.common;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class BakaPotatoModListFormatter {
    private BakaPotatoModListFormatter() {
    }

    public static List<String> format(List<ModEntry> mods, BakaPotatoClientConfig config) {
        if (!config.privacy.sendModList) {
            return List.of();
        }
        boolean necessaryOnly = BakaPotatoClientConfig.PrivacyConfig.MOD_LIST_NECESSARY.equals(config.privacy.modListMode);
        return mods.stream()
                .filter(entry -> !necessaryOnly || !isLoaderMod(entry.id(), entry.name()))
                .map(entry -> necessaryOnly ? entry.name() : entry.id() + "@" + entry.version())
                .filter(value -> !value.isBlank())
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    private static boolean isLoaderMod(String id, String name) {
        String lowerId = Objects.requireNonNullElse(id, "").toLowerCase(Locale.ROOT);
        String lowerName = Objects.requireNonNullElse(name, "").toLowerCase(Locale.ROOT);
        if (lowerId.equals(BakaPotatoProtocol.MOD_ID)) {
            return true;
        }
        if (lowerId.equals("minecraft") || lowerId.equals("java") || lowerId.equals("fabricloader")
                || lowerId.equals("forge") || lowerId.equals("neoforge") || lowerId.equals("fmlcore")
                || lowerId.equals("javafmllanguage") || lowerId.equals("lowcodelanguage")
                || lowerId.equals("mclanguage")) {
            return true;
        }
        return lowerId.startsWith("fabric-")
                || lowerId.startsWith("fabric_")
                || lowerId.startsWith("forge-")
                || lowerId.startsWith("neoforge-")
                || lowerName.contains("fabric api");
    }

    public record ModEntry(String id, String name, String version) {
        public ModEntry {
            id = id == null ? "" : id;
            name = name == null || name.isBlank() ? id : name;
            version = version == null ? "" : version;
        }
    }
}
