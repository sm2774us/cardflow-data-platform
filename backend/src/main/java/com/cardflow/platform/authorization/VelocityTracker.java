package com.cardflow.platform.authorization;

import java.util.ArrayDeque;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** Sliding one-minute window per card token. Per-replica; production would use Redis/Kafka-Streams state for global counts. */
@Component
public class VelocityTracker {
    private static final long WINDOW_MS = 60_000;
    private final ConcurrentHashMap<String, ArrayDeque<Long>> windows = new ConcurrentHashMap<>();

    public int recordAndCount(String token, long nowMs) {
        ArrayDeque<Long> q = windows.computeIfAbsent(token, k -> new ArrayDeque<>());
        synchronized (q) {
            while (!q.isEmpty() && nowMs - q.peekFirst() > WINDOW_MS) q.pollFirst();
            q.addLast(nowMs);
            int n = q.size();
            if (windows.size() > 50_000) windows.entrySet().removeIf(e -> e.getValue().isEmpty());
            return n;
        }
    }
}
