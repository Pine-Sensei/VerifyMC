package team.kitemc.verifymc.infrastructure.web;

import org.bukkit.plugin.Plugin;

import java.util.logging.Level;

public class LoggingMiddleware implements Middleware {
    private final Plugin plugin;
    private final boolean logRequestBody;
    private final boolean logResponseBody;

    public LoggingMiddleware(Plugin plugin) {
        this(plugin, false, false);
    }

    public LoggingMiddleware(Plugin plugin, boolean logRequestBody, boolean logResponseBody) {
        this.plugin = plugin;
        this.logRequestBody = logRequestBody;
        this.logResponseBody = logResponseBody;
    }

    @Override
    public void handle(RequestContext ctx, MiddlewareChain next) throws Exception {
        long startTime = System.currentTimeMillis();
        String requestId = ctx.getRequestId();
        String method = ctx.getMethod();
        String path = ctx.getPath();
        String clientIp = ctx.getClientIp();

        StringBuilder logBuilder = new StringBuilder();
        logBuilder.append("[HTTP] ").append(requestId).append(" ");
        logBuilder.append(method).append(" ").append(path);
        logBuilder.append(" from ").append(clientIp);

        if (logRequestBody && ("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method))) {
            String body = ctx.getBody();
            if (body != null && !body.isEmpty()) {
                logBuilder.append(" | Body: ").append(truncate(body, 500));
            }
        }

        plugin.getLogger().log(Level.INFO, logBuilder.toString());

        try {
            next.next();

            long elapsed = System.currentTimeMillis() - startTime;
            String responseLog = String.format("[HTTP] %s %s %s completed in %dms",
                    requestId, method, path, elapsed);
            plugin.getLogger().log(Level.INFO, responseLog);

        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            String errorLog = String.format("[HTTP] %s %s %s failed in %dms: %s",
                    requestId, method, path, elapsed, e.getMessage());
            plugin.getLogger().log(Level.WARNING, errorLog, e);
            throw e;
        }
    }

    private String truncate(String str, int maxLength) {
        if (str == null) {
            return "";
        }
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength) + "...";
    }
}
