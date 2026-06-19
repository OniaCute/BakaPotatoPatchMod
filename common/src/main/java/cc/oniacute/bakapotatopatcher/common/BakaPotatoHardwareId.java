package cc.oniacute.bakapotatopatcher.common;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

public final class BakaPotatoHardwareId {
    private static final AtomicReference<String> CACHE = new AtomicReference<>();

    private BakaPotatoHardwareId() {
    }

    public static String sha256() {
        String cached = CACHE.get();
        if (cached != null) {
            return cached;
        }
        String source = String.join("|",
                queryWindowsWmic("cpu", "ProcessorId"),
                queryWindowsWmic("baseboard", "SerialNumber"),
                System.getenv("PROCESSOR_IDENTIFIER"),
                System.getProperty("os.name", ""),
                System.getProperty("os.arch", "")
        );
        String hash = sha256Hex(source);
        CACHE.compareAndSet(null, hash);
        return CACHE.get();
    }

    private static String queryWindowsWmic(String alias, String property) {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (!os.contains("win")) {
            return "";
        }
        Process process = null;
        try {
            process = new ProcessBuilder("wmic", alias, "get", property)
                    .redirectErrorStream(true)
                    .start();
            if (!process.waitFor(1500, java.util.concurrent.TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                return "";
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                return reader.lines()
                        .map(String::trim)
                        .filter(line -> !line.isEmpty())
                        .filter(line -> !line.equalsIgnoreCase(property))
                        .findFirst()
                        .orElse("");
            }
        } catch (Exception ignored) {
            if (process != null) {
                process.destroyForcibly();
            }
            return "";
        }
    }

    private static String sha256Hex(String source) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(source.getBytes(StandardCharsets.UTF_8));
            StringBuilder output = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                output.append(String.format("%02x", value));
            }
            return output.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to hash hardware id.", exception);
        }
    }
}
