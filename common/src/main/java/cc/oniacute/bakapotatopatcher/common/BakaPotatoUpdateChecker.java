package cc.oniacute.bakapotatopatcher.common;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BakaPotatoUpdateChecker {
    public static final String UPDATE_API_URL = "https://mods.bakapotato.com/mods/ver";
    public static final String DOWNLOAD_URL = "https://mods.bakapotato.com/";

    private static final Pattern VERSION_PATTERN = Pattern.compile("\"version\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern REMINDS_PATTERN = Pattern.compile("\"?reminds\"?\\s*:\\s*(\\d+)");
    private static final Pattern OK_PATTERN = Pattern.compile("\"ok\"\\s*:\\s*(true|false)");
    private static final Pattern CODE_PATTERN = Pattern.compile("\"code\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern MESSAGE_PATTERN = Pattern.compile("\"message\"\\s*:\\s*\"([^\"]*)\"");
    private static final AtomicReference<Result> lastResult = new AtomicReference<>(Result.idle());
    private static final AtomicBoolean inProgress = new AtomicBoolean();
    private static final AtomicBoolean startupReminderConsumed = new AtomicBoolean();
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(6))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private BakaPotatoUpdateChecker() {
    }

    public static void checkAsync(String loaderId, String currentVersion, boolean startup) {
        if (!inProgress.compareAndSet(false, true)) {
            return;
        }
        lastResult.set(Result.checking(loaderId, currentVersion));
        CompletableFuture.runAsync(() -> {
            try {
                Result result = fetch(loaderId, currentVersion);
                lastResult.set(result);
                if (startup && result.shouldRemind()) {
                    startupReminderConsumed.set(false);
                }
            } catch (RuntimeException exception) {
                lastResult.set(Result.failed(loaderId, currentVersion, exception.getMessage()));
            } finally {
                inProgress.set(false);
            }
        });
    }

    public static Result lastResult() {
        return lastResult.get();
    }

    public static void markDisabled(String loaderId, String currentVersion) {
        lastResult.set(new Result(Status.DISABLED, loaderId, currentVersion, "", 1, true, ""));
    }

    public static boolean consumeStartupReminder() {
        Result result = lastResult.get();
        return result.shouldRemind() && startupReminderConsumed.compareAndSet(false, true);
    }

    private static Result fetch(String loaderId, String currentVersion) {
        String normalizedLoader = Objects.requireNonNullElse(loaderId, "").toLowerCase(Locale.ROOT);
        HttpRequest request = HttpRequest.newBuilder(URI.create(UPDATE_API_URL))
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build();
        try {
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return Result.failed(normalizedLoader, currentVersion, "HTTP " + response.statusCode());
            }
            String body = response.body();
            String ok = match(OK_PATTERN, body);
            if ("false".equals(ok)) {
                String code = match(CODE_PATTERN, body);
                String message = match(MESSAGE_PATTERN, body);
                String detail = code.isBlank() ? message : code + (message.isBlank() ? "" : ": " + message);
                return Result.failed(normalizedLoader, currentVersion, detail.isBlank() ? "api error" : detail);
            }
            String object = loaderObject(body, normalizedLoader);
            if (object.isBlank()) {
                return Result.failed(normalizedLoader, currentVersion, "missing loader entry");
            }
            String latestVersion = match(VERSION_PATTERN, object);
            String remindsText = match(REMINDS_PATTERN, object);
            int reminds = remindsText.isBlank() ? 1 : Integer.parseInt(remindsText);
            boolean latest = compareVersions(currentVersion, latestVersion) >= 0;
            return new Result(Status.READY, normalizedLoader, currentVersion, latestVersion, reminds, latest, "");
        } catch (IOException | InterruptedException | IllegalArgumentException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return Result.failed(normalizedLoader, currentVersion, exception.getMessage());
        }
    }

    private static String loaderObject(String json, String loaderId) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(loaderId) + "\"\\s*:\\s*\\{([^}]*)}");
        Matcher matcher = pattern.matcher(json);
        return matcher.find() ? matcher.group(1) : "";
    }

    private static String match(Pattern pattern, String value) {
        Matcher matcher = pattern.matcher(value);
        return matcher.find() ? matcher.group(1) : "";
    }

    public static int compareVersions(String current, String latest) {
        String[] left = Objects.requireNonNullElse(current, "").split("[^0-9A-Za-z]+");
        String[] right = Objects.requireNonNullElse(latest, "").split("[^0-9A-Za-z]+");
        int length = Math.max(left.length, right.length);
        for (int index = 0; index < length; index++) {
            String a = index < left.length ? left[index] : "0";
            String b = index < right.length ? right[index] : "0";
            int compared = comparePart(a, b);
            if (compared != 0) {
                return compared;
            }
        }
        return 0;
    }

    private static int comparePart(String left, String right) {
        boolean leftNumber = left.matches("\\d+");
        boolean rightNumber = right.matches("\\d+");
        if (leftNumber && rightNumber) {
            return Integer.compare(Integer.parseInt(left), Integer.parseInt(right));
        }
        return left.compareToIgnoreCase(right);
    }

    public enum Status {
        IDLE,
        DISABLED,
        CHECKING,
        READY,
        FAILED
    }

    public record Result(
            Status status,
            String loader,
            String currentVersion,
            String latestVersion,
            int reminds,
            boolean latest,
            String error
    ) {
        public static Result idle() {
            return new Result(Status.IDLE, "", "", "", 1, true, "");
        }

        public static Result checking(String loader, String currentVersion) {
            return new Result(Status.CHECKING, loader, currentVersion, "", 1, true, "");
        }

        public static Result failed(String loader, String currentVersion, String error) {
            return new Result(Status.FAILED, loader, currentVersion, "", 1, true, error == null ? "unknown" : error);
        }

        public boolean shouldRemind() {
            return status == Status.READY && (!latest || reminds == 1);
        }
    }
}
