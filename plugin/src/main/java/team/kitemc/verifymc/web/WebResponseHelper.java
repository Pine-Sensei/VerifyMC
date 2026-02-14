package team.kitemc.verifymc.web;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
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
}
