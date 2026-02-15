package team.kitemc.verifymc.infrastructure.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StaticFileHandler implements HttpHandler {
    private static final Logger LOGGER = Logger.getLogger(StaticFileHandler.class.getName());
    private static final Map<String, String> MIME_TYPES = new HashMap<>();

    static {
        MIME_TYPES.put("html", "text/html");
        MIME_TYPES.put("htm", "text/html");
        MIME_TYPES.put("css", "text/css");
        MIME_TYPES.put("js", "application/javascript");
        MIME_TYPES.put("json", "application/json");
        MIME_TYPES.put("xml", "text/xml");
        MIME_TYPES.put("svg", "image/svg+xml");
        MIME_TYPES.put("png", "image/png");
        MIME_TYPES.put("jpg", "image/jpeg");
        MIME_TYPES.put("jpeg", "image/jpeg");
        MIME_TYPES.put("gif", "image/gif");
        MIME_TYPES.put("ico", "image/x-icon");
        MIME_TYPES.put("woff", "font/woff");
        MIME_TYPES.put("woff2", "font/woff2");
        MIME_TYPES.put("ttf", "font/ttf");
        MIME_TYPES.put("eot", "application/vnd.ms-fontobject");
    }

    private String baseDir;
    private boolean spaFallbackEnabled = true;

    public StaticFileHandler() {
    }

    public StaticFileHandler(String baseDir) {
        this.baseDir = baseDir;
    }

    public void setBaseDir(String dir) {
        this.baseDir = dir;
    }

    public void setSpaFallbackEnabled(boolean enabled) {
        this.spaFallbackEnabled = enabled;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String uri = URLDecoder.decode(exchange.getRequestURI().getPath(), StandardCharsets.UTF_8);
        if (uri.equals("/")) {
            uri = "/index.html";
        }

        Path file = Paths.get(baseDir, uri);

        try {
            if (!isPathSafe(file)) {
                serve404(exchange);
                return;
            }

            if (!Files.exists(file) || Files.isDirectory(file)) {
                if (spaFallbackEnabled && !uri.startsWith("/api/")) {
                    serveSpaFallback(exchange);
                    return;
                }
                serve404(exchange);
                return;
            }

            serveFile(exchange, file);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error serving static file: " + uri, e);
            serve500(exchange);
        }
    }

    private boolean isPathSafe(Path file) {
        Path normalizedBase = Paths.get(baseDir).normalize();
        Path normalizedFile = file.normalize();
        return normalizedFile.startsWith(normalizedBase);
    }

    private void serveFile(HttpExchange exchange, Path file) throws IOException {
        String contentType = getMimeType(file);
        byte[] data = Files.readAllBytes(file);

        exchange.getResponseHeaders().add("Content-Type", contentType);
        exchange.sendResponseHeaders(200, data.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(data);
        }
    }

    private void serveSpaFallback(HttpExchange exchange) throws IOException {
        Path indexFile = Paths.get(baseDir, "index.html");
        if (Files.exists(indexFile)) {
            serveFile(exchange, indexFile);
        } else {
            serve404(exchange);
        }
    }

    private void serve404(HttpExchange exchange) throws IOException {
        Path notFoundPage = Paths.get(baseDir, "404.html");
        if (Files.exists(notFoundPage)) {
            byte[] data = Files.readAllBytes(notFoundPage);
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(404, data.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(data);
            }
        } else {
            String message = "404 Not Found";
            exchange.sendResponseHeaders(404, message.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(message.getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    private void serve500(HttpExchange exchange) throws IOException {
        Path errorPage = Paths.get(baseDir, "500.html");
        if (Files.exists(errorPage)) {
            byte[] data = Files.readAllBytes(errorPage);
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(500, data.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(data);
            }
        } else {
            String message = "500 Internal Server Error";
            exchange.sendResponseHeaders(500, message.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(message.getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    private String getMimeType(Path file) {
        String fileName = file.getFileName().toString().toLowerCase();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            String extension = fileName.substring(dotIndex + 1);
            String mime = MIME_TYPES.get(extension);
            if (mime != null) {
                return addCharset(mime);
            }
        }

        try {
            String probeMime = Files.probeContentType(file);
            if (probeMime != null) {
                return addCharset(probeMime);
            }
        } catch (IOException ignored) {
        }

        return "application/octet-stream";
    }

    private String addCharset(String mimeType) {
        if (mimeType.startsWith("text/") ||
            mimeType.equals("application/javascript") ||
            mimeType.equals("application/json") ||
            mimeType.equals("application/xml") ||
            mimeType.equals("text/xml") ||
            mimeType.equals("image/svg+xml")) {
            return mimeType + "; charset=utf-8";
        }
        return mimeType;
    }
}
