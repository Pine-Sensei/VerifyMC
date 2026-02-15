package team.kitemc.verifymc.infrastructure.web;

import org.bukkit.plugin.Plugin;
import org.json.JSONObject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class RateLimitMiddleware implements Middleware {
    private final Plugin plugin;
    private final int maxRequests;
    private final long windowMs;
    private final KeyExtractor keyExtractor;
    private final Map<String, RateLimitEntry> rateLimitStore = new ConcurrentHashMap<>();

    public RateLimitMiddleware(Plugin plugin, int maxRequests, long windowMs) {
        this(plugin, maxRequests, windowMs, ctx -> ctx.getClientIp());
    }

    public RateLimitMiddleware(Plugin plugin, int maxRequests, long windowMs, KeyExtractor keyExtractor) {
        this.plugin = plugin;
        this.maxRequests = maxRequests;
        this.windowMs = windowMs;
        this.keyExtractor = keyExtractor;
    }

    @Override
    public void handle(RequestContext ctx) throws Exception {
        String key = keyExtractor.extract(ctx);
        long now = System.currentTimeMillis();

        RateLimitEntry entry = rateLimitStore.compute(key, (k, old) -> {
            if (old == null || now - old.windowStart >= windowMs) {
                return new RateLimitEntry(1, now);
            }
            old.count.incrementAndGet();
            return old;
        });

        int remaining = Math.max(0, maxRequests - entry.count.get());
        long retryAfter = Math.max(0, windowMs - (now - entry.windowStart));

        ctx.getRawExchange().getResponseHeaders().set("X-RateLimit-Limit", String.valueOf(maxRequests));
        ctx.getRawExchange().getResponseHeaders().set("X-RateLimit-Remaining", String.valueOf(remaining));
        ctx.getRawExchange().getResponseHeaders().set("X-RateLimit-Reset", String.valueOf(retryAfter));

        if (entry.count.get() > maxRequests) {
            ctx.getRawExchange().getResponseHeaders().set("Retry-After", String.valueOf(retryAfter / 1000));

            JSONObject errorResponse = new JSONObject();
            errorResponse.put("success", false);
            errorResponse.put("error", "rate_limit_exceeded");
            errorResponse.put("message", "Too many requests. Please try again later.");
            errorResponse.put("retry_after_ms", retryAfter);

            ctx.sendJson(429, errorResponse);
            return;
        }
    }

    public void cleanup() {
        long now = System.currentTimeMillis();
        rateLimitStore.entrySet().removeIf(entry -> now - entry.getValue().windowStart >= windowMs * 2);
    }

    public void reset(String key) {
        rateLimitStore.remove(key);
    }

    public void resetAll() {
        rateLimitStore.clear();
    }

    @FunctionalInterface
    public interface KeyExtractor {
        String extract(RequestContext ctx);
    }

    private static class RateLimitEntry {
        final AtomicInteger count;
        final long windowStart;

        RateLimitEntry(int count, long windowStart) {
            this.count = new AtomicInteger(count);
            this.windowStart = windowStart;
        }
    }

    public static KeyExtractor byIp() {
        return ctx -> ctx.getClientIp();
    }

    public static KeyExtractor byIpAndPath() {
        return ctx -> ctx.getClientIp() + ":" + ctx.getPath();
    }

    public static KeyExtractor byHeader(String headerName) {
        return ctx -> {
            String value = ctx.getHeader(headerName);
            return value != null ? value : ctx.getClientIp();
        };
    }

    public static KeyExtractor byQueryParam(String paramName) {
        return ctx -> {
            String value = ctx.getQueryParam(paramName);
            return value != null ? value : ctx.getClientIp();
        };
    }
}
