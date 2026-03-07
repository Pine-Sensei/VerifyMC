package team.kitemc.verifymc.web;

import com.sun.net.httpserver.HttpExchange;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Utility class for HTTP response handling.
 * Consolidates JSON read/write and method enforcement.
 * (Preserved from original for backward compatibility.)
 */
public final class WebResponseHelper {
    private WebResponseHelper() {}

    /**
     * Read the request body as a JSONObject.
     * @param exchange the HTTP exchange
     * @return JSONObject parsed from request body, or empty JSONObject if body is empty
     * @throws IOException if an I/O error occurs
     * @throws JSONException if the request body contains invalid JSON
     */
    public static JSONObject readJson(HttpExchange exchange) throws IOException, JSONException {
        try (InputStream is = exchange.getRequestBody();
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            String body = sb.toString().trim();
            if (body.isEmpty()) {
                return new JSONObject();
            }
            return new JSONObject(body);  // Let JSONException propagate to caller
        }
    }

    /**
     * Send a JSON response with HTTP 200.
     */
    public static void sendJson(HttpExchange exchange, JSONObject json) throws IOException {
        sendJson(exchange, json, 200);
    }

    /**
     * Send a JSON response with the specified HTTP status code.
     */
    public static void sendJson(HttpExchange exchange, JSONObject json, int statusCode) throws IOException {
        byte[] bytes = json.toString().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.getResponseHeaders().set("X-Frame-Options", "DENY");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    /**
     * Enforce the HTTP method. If the exchange does not match, sends 405 and returns false.
     */
    public static boolean requireMethod(HttpExchange exchange, String method) throws IOException {
        if (exchange.getRequestMethod().equalsIgnoreCase(method)) {
            return true;
        }
        JSONObject error = new JSONObject().put("success", false).put("msg", "Method Not Allowed");
        sendJson(exchange, error, 405);
        return false;
    }
}
