package team.kitemc.verifymc.web.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import team.kitemc.verifymc.core.PluginContext;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Serves static files (front-end HTML/CSS/JS) from the plugin data directory.
 * This replaces the inline StaticHandler class from the original WebServer.
 */
public class StaticFileHandler implements HttpHandler {
    private final PluginContext ctx;

    public StaticFileHandler(PluginContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String relativePath = toRelativePath(exchange.getRequestURI().getPath());

        // Determine the theme directory
        String theme = ctx.getConfigManager().getTheme();
        File staticDir = ctx.getResourceManager().getStaticDir();
        File themeDir = new File(staticDir, theme);

        // If theme dir doesn't exist, fall back to the static root
        if (!themeDir.exists()) {
            themeDir = staticDir;
        }

        Path themeDirPath = themeDir.toPath().toAbsolutePath().normalize();
        Path requestPath = resolveWithinBase(themeDirPath, relativePath);
        if (requestPath == null) {
            exchange.sendResponseHeaders(403, -1);
            return;
        }

        File file = requestPath.toFile();

        if (!file.exists() || file.isDirectory()) {
            // Try serving index.html for SPA routing
            Path indexPath = resolveWithinBase(themeDirPath, "index.html");
            if (indexPath == null) {
                exchange.sendResponseHeaders(403, -1);
                return;
            }
            File indexFile = indexPath.toFile();
            if (indexFile.exists()) {
                serveFile(exchange, indexFile);
            } else {
                String msg = "404 Not Found";
                byte[] bytes = msg.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(404, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            }
            return;
        }

        serveFile(exchange, file);
    }

    static String toRelativePath(String uriPath) {
        if (uriPath == null || uriPath.isEmpty() || "/".equals(uriPath)) {
            return "index.html";
        }
        String relativePath = uriPath;
        while (relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1);
        }
        return relativePath.isEmpty() ? "index.html" : relativePath;
    }

    static Path resolveWithinBase(Path basePath, String relativePath) {
        Path candidate = basePath.resolve(relativePath).normalize();
        if (!candidate.startsWith(basePath)) {
            return null;
        }
        return candidate;
    }

    private void serveFile(HttpExchange exchange, File file) throws IOException {
        String contentType = determineContentType(file.getName());
        exchange.getResponseHeaders().set("Content-Type", contentType);

        byte[] data = Files.readAllBytes(file.toPath());
        exchange.sendResponseHeaders(200, data.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(data);
        }
    }

    private String determineContentType(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".html")) return "text/html; charset=utf-8";
        if (lower.endsWith(".css")) return "text/css; charset=utf-8";
        if (lower.endsWith(".js")) return "application/javascript; charset=utf-8";
        if (lower.endsWith(".json")) return "application/json; charset=utf-8";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        if (lower.endsWith(".ico")) return "image/x-icon";
        if (lower.endsWith(".woff")) return "font/woff";
        if (lower.endsWith(".woff2")) return "font/woff2";
        if (lower.endsWith(".ttf")) return "font/ttf";
        return "application/octet-stream";
    }
}
