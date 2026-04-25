package team.kitemc.verifymc.web;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import team.kitemc.verifymc.core.PluginContext;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

/**
 * Shared CORS policy helpers for API endpoints.
 */
public final class CorsSupport {
    private static final String ALLOWED_METHODS = "GET, POST, PUT, PATCH, DELETE, OPTIONS";
    private static final String ALLOWED_HEADERS = "Content-Type, Authorization";
    private static final String VARY_VALUE = "Origin, Access-Control-Request-Method, Access-Control-Request-Headers";

    private CorsSupport() {
    }

    public static boolean hasOriginHeader(HttpExchange exchange) {
        String requestOrigin = exchange.getRequestHeaders().getFirst("Origin");
        String normalizedRequestOrigin = normalizeOrigin(requestOrigin);
        return !normalizedRequestOrigin.isEmpty();
    }

    public static boolean isOriginAllowed(HttpExchange exchange, List<String> allowedOrigins) {
        String requestOrigin = exchange.getRequestHeaders().getFirst("Origin");
        String normalizedRequestOrigin = normalizeOrigin(requestOrigin);
        if (normalizedRequestOrigin.isEmpty()) {
            return false;
        }
        if (allowedOrigins == null || allowedOrigins.isEmpty()) {
            return true;
        }
        for (String allowedOrigin : allowedOrigins) {
            if (matchesOrigin(normalizedRequestOrigin, allowedOrigin)) {
                return true;
            }
        }
        return false;
    }

    public static void applyCorsHeaders(PluginContext ctx, HttpExchange exchange, List<String> allowedOrigins) {
        String requestOrigin = exchange.getRequestHeaders().getFirst("Origin");
        if (!hasOriginHeader(exchange)) {
            return;
        }
        if (!isOriginAllowed(exchange, allowedOrigins)) {
            logRejectedOrigin(ctx, exchange, requestOrigin);
            return;
        }

        Headers headers = exchange.getResponseHeaders();
        headers.set("Access-Control-Allow-Origin", requestOrigin);
        headers.set("Access-Control-Allow-Methods", ALLOWED_METHODS);
        headers.set("Access-Control-Allow-Headers", ALLOWED_HEADERS);
        headers.set("Vary", VARY_VALUE);
    }

    public static boolean handlePreflight(PluginContext ctx, HttpExchange exchange, List<String> allowedOrigins) throws IOException {
        if (!"OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            return false;
        }

        String requestOrigin = exchange.getRequestHeaders().getFirst("Origin");
        if (!isOriginAllowed(exchange, allowedOrigins)) {
            logRejectedOrigin(ctx, exchange, requestOrigin);
            exchange.sendResponseHeaders(403, -1);
            return true;
        }

        applyCorsHeaders(ctx, exchange, allowedOrigins);
        exchange.sendResponseHeaders(204, -1);
        return true;
    }

    private static String normalizeOrigin(String origin) {
        if (origin == null) {
            return "";
        }
        return origin.trim().replaceAll("/+$", "");
    }

    private static boolean matchesOrigin(String requestOrigin, String allowedOriginPattern) {
        try {
            URI requestOriginUri = URI.create(requestOrigin);
            URI allowedOriginUri = URI.create(normalizeOrigin(allowedOriginPattern).replace("*.", "wildcard."));
            String requestScheme = normalizeScheme(requestOriginUri.getScheme());
            String allowedScheme = normalizeScheme(allowedOriginUri.getScheme());
            String requestHost = normalizeHost(requestOriginUri.getHost());
            String allowedHost = normalizeHost(allowedOriginUri.getHost());
            if (requestScheme.isEmpty() || allowedScheme.isEmpty() || requestHost.isEmpty() || allowedHost.isEmpty()) {
                return false;
            }
            if (!requestScheme.equals(allowedScheme)) {
                return false;
            }
            if (!allowedHost.startsWith("wildcard.")) {
                return requestHost.equals(allowedHost);
            }
            return matchesWildcardSubdomain(requestHost, allowedHost);
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private static boolean matchesWildcardSubdomain(String requestHost, String allowedHost) {
        if (!allowedHost.startsWith("wildcard.")) {
            return false;
        }
        String suffix = allowedHost.substring("wildcard".length());
        if (!requestHost.endsWith(suffix)) {
            return false;
        }
        String prefix = requestHost.substring(0, requestHost.length() - suffix.length());
        return !prefix.isEmpty() && prefix.indexOf('.') == -1;
    }

    private static String normalizeScheme(String scheme) {
        if (scheme == null) {
            return "";
        }
        return scheme.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeHost(String host) {
        if (host == null) {
            return "";
        }
        String normalizedHost = host.trim().toLowerCase(Locale.ROOT);
        int colonIndex = normalizedHost.indexOf(':');
        if (colonIndex >= 0) {
            normalizedHost = normalizedHost.substring(0, colonIndex);
        }
        return normalizedHost;
    }

    private static void logRejectedOrigin(PluginContext ctx, HttpExchange exchange, String requestOrigin) {
        if (ctx == null) {
            return;
        }
        String remoteAddress = exchange.getRemoteAddress() == null ? "" : String.valueOf(exchange.getRemoteAddress());
        String path = exchange.getRequestURI() == null ? "" : exchange.getRequestURI().toString();
        ctx.getPlugin().getLogger().log(
            Level.WARNING,
            "Rejected CORS origin: origin={0}, method={1}, path={2}, remote={3}",
            new Object[]{normalizeOrigin(requestOrigin), exchange.getRequestMethod(), path, remoteAddress}
        );
    }
}
