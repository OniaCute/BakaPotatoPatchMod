package cc.oniacute.bakapotatopatcher.common;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.List;
import java.util.ArrayList;
import java.util.Deque;

public final class BakaPotatoDebugInfo {
    private static final Deque<Entry> ENTRIES = new ArrayDeque<>();
    private static final int HARD_LIMIT = 128;

    private BakaPotatoDebugInfo() {
    }

    public static List<String> hudLines(BakaPotatoClientConfig config) {
        List<String> lines = new ArrayList<>();
        if (config == null || config.debug == null || !config.debug.showHud) {
            return lines;
        }
        long now = System.currentTimeMillis();
        long ttl = config.debug.hudStaySeconds * 1000L;
        synchronized (ENTRIES) {
            ENTRIES.removeIf(entry -> now - entry.createdAtMillis > ttl);
            int count = 0;
            for (Entry entry : ENTRIES) {
                lines.add(entry.text);
                count++;
                if (count >= config.debug.hudMaxLines) {
                    break;
                }
            }
        }
        return lines;
    }

    public static void log(String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        String line = "[" + Instant.now().toString().substring(11, 19) + "] " + message;
        synchronized (ENTRIES) {
            ENTRIES.addFirst(new Entry(line, System.currentTimeMillis()));
            while (ENTRIES.size() > HARD_LIMIT) {
                ENTRIES.removeLast();
            }
        }
    }

    private record Entry(String text, long createdAtMillis) {
    }
}
