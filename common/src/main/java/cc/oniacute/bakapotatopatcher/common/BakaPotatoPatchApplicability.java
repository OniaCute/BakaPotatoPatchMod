package cc.oniacute.bakapotatopatcher.common;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class BakaPotatoPatchApplicability {
    private static final AtomicReference<Set<String>> REMOTE_SERVERS = new AtomicReference<>(Set.of());
    private static final AtomicReference<String> CURRENT_SERVER = new AtomicReference<>("");
    private static final AtomicBoolean FETCH_STARTED = new AtomicBoolean(false);

    private BakaPotatoPatchApplicability() {
    }

    public static void refreshRemoteServersAsync(BakaPotatoClientConfig config) {
        if (config == null || config.patches == null || !FETCH_STARTED.compareAndSet(false, true)) {
            return;
        }
        String url = config.patches.remoteDomainListUrl;
        CompletableFuture.runAsync(() -> {
            try {
                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(5))
                        .build();
                HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                        .timeout(Duration.ofSeconds(8))
                        .GET()
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    REMOTE_SERVERS.set(Collections.unmodifiableSet(parseServerAddresses(response.body())));
                }
            } catch (IllegalArgumentException | IOException | InterruptedException exception) {
                if (exception instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
            }
        });
    }

    public static void setCurrentServerAddress(String address) {
        CURRENT_SERVER.set(normalizeAddress(address));
    }

    public static boolean isEnabledForCurrentServer(BakaPotatoClientConfig config) {
        if (config == null || config.patches == null) {
            return false;
        }
        if (BakaPotatoClientConfig.PatchConfig.APPLY_MODE_ALL_SERVERS.equals(config.patches.applyMode)) {
            return true;
        }
        String current = CURRENT_SERVER.get();
        return !current.isEmpty() && REMOTE_SERVERS.get().contains(current);
    }

    public static Set<String> remoteServers() {
        return REMOTE_SERVERS.get();
    }

    public static String currentServer() {
        return CURRENT_SERVER.get();
    }

    private static Set<String> parseServerAddresses(String body) {
        LinkedHashSet<String> addresses = new LinkedHashSet<>();
        JsonElement root = JsonParser.parseString(body);
        collectAddresses(root, addresses);
        return addresses;
    }

    private static void collectAddresses(JsonElement element, Set<String> addresses) {
        if (element == null || element.isJsonNull()) {
            return;
        }
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            addAddress(element.getAsString(), addresses);
            return;
        }
        if (element.isJsonArray()) {
            element.getAsJsonArray().forEach(child -> collectAddresses(child, addresses));
            return;
        }
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            for (String key : object.keySet()) {
                JsonElement child = object.get(key);
                if (isAddressField(key) && child.isJsonPrimitive() && child.getAsJsonPrimitive().isString()) {
                    addAddress(child.getAsString(), addresses);
                } else {
                    collectAddresses(child, addresses);
                }
            }
        }
    }

    private static boolean isAddressField(String key) {
        String normalized = key.toLowerCase(Locale.ROOT);
        return normalized.equals("address")
                || normalized.equals("ip")
                || normalized.equals("host")
                || normalized.equals("domain")
                || normalized.equals("server")
                || normalized.equals("url");
    }

    private static void addAddress(String value, Set<String> addresses) {
        String normalized = normalizeAddress(value);
        if (!normalized.isEmpty()) {
            addresses.add(normalized);
        }
    }

    private static String normalizeAddress(String address) {
        if (address == null) {
            return "";
        }
        String normalized = address.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("minecraft://")) {
            normalized = normalized.substring("minecraft://".length());
        }
        if (normalized.startsWith("http://")) {
            normalized = normalized.substring("http://".length());
        } else if (normalized.startsWith("https://")) {
            normalized = normalized.substring("https://".length());
        }
        int slash = normalized.indexOf('/');
        if (slash >= 0) {
            normalized = normalized.substring(0, slash);
        }
        if (normalized.endsWith(".")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
