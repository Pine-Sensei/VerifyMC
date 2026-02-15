package team.kitemc.verifymc.web;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;

public final class WebResponseHelper {
    private WebResponseHelper() {
    }

    public static JSONObject readJson(HttpExchange exchange) throws IOException {
        return new JSONObject(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
    }

    public static void sendJson(HttpExchange exchange, JSONObject response) throws IOException {
        byte[] data = response.toString().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(200, data.length);
        exchange.getResponseBody().write(data);
        exchange.close();
    }

    public static boolean requireMethod(HttpExchange exchange, String method) throws IOException {
        if (method.equals(exchange.getRequestMethod())) {
            return true;
        }
        exchange.sendResponseHeaders(405, 0);
        exchange.close();
        return false;
    }

    public static void sendSuccess(HttpExchange exchange, String msg) throws IOException {
        sendJson(exchange, 200, ApiResponse.success(msg).toJson());
    }

    public static void sendSuccess(HttpExchange exchange, String msg, Object data) throws IOException {
        sendJson(exchange, 200, ApiResponse.success(msg, data).toJson());
    }

    public static void sendError(HttpExchange exchange, int statusCode, String msg) throws IOException {
        sendJson(exchange, statusCode, ApiResponse.failure(msg).toJson());
    }

    public static void sendError(HttpExchange exchange, int statusCode, String msg, String code) throws IOException {
        sendJson(exchange, statusCode, ApiResponse.failure(msg, code).toJson());
    }

    public static void sendBadRequest(HttpExchange exchange, String msg) throws IOException {
        sendError(exchange, 400, msg);
    }

    public static void sendUnauthorized(HttpExchange exchange, String msg) throws IOException {
        sendError(exchange, 401, msg, ErrorCode.UNAUTHORIZED.getCode());
    }

    public static void sendForbidden(HttpExchange exchange, String msg) throws IOException {
        sendError(exchange, 403, msg, ErrorCode.FORBIDDEN.getCode());
    }

    public static void sendNotFound(HttpExchange exchange, String msg) throws IOException {
        sendError(exchange, 404, msg, ErrorCode.NOT_FOUND.getCode());
    }

    public static void sendInternalError(HttpExchange exchange) throws IOException {
        sendError(exchange, 500, "Internal server error", ErrorCode.INTERNAL_ERROR.getCode());
    }

    public static void sendJson(HttpExchange exchange, int statusCode, String json) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
