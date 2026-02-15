package team.kitemc.verifymc.infrastructure.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;
import team.kitemc.verifymc.infrastructure.exception.GlobalExceptionHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ApiHandler implements HttpHandler {
    private static final Logger LOGGER = Logger.getLogger(ApiHandler.class.getName());

    private Router router;
    private String copyright = "Powered by VerifyMC (GPLv3)";

    public ApiHandler() {
    }

    public ApiHandler(Router router) {
        this.router = router;
    }

    public void setRouter(Router router) {
        this.router = router;
    }

    public void setCopyright(String copyright) {
        this.copyright = copyright;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        String clientIp = getClientIp(exchange);

        LOGGER.log(Level.FINE, "API Request: {0} {1} from {2}", new Object[]{method, path, clientIp});

        try {
            if (router == null) {
                sendError(exchange, 500, "Router not configured");
                return;
            }

            RouteMatch match = router.match(method, path);
            if (match == null) {
                sendError(exchange, 404, "Not Found");
                return;
            }

            RequestContext context = new RequestContext(exchange, match.getParams());

            for (Middleware middleware : router.getMiddlewares()) {
                if (!middleware.intercept(context)) {
                    return;
                }
            }

            RouteHandler handler = match.getHandler();
            ApiResponse response = handler.handle(context);

            sendResponse(exchange, response);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error handling API request: " + path, e);
            ApiResponse response = GlobalExceptionHandler.handle(e);
            sendResponse(exchange, response, GlobalExceptionHandler.getHttpStatus(e));
        }
    }

    private String getClientIp(HttpExchange exchange) {
        String forwarded = exchange.getRequestHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        if (exchange.getRemoteAddress() != null && exchange.getRemoteAddress().getAddress() != null) {
            return exchange.getRemoteAddress().getAddress().getHostAddress();
        }
        return "unknown";
    }

    private void sendResponse(HttpExchange exchange, ApiResponse response) throws IOException {
        sendResponse(exchange, response, 200);
    }

    private void sendResponse(HttpExchange exchange, ApiResponse response, int statusCode) throws IOException {
        JSONObject json = response.toJSONObject();
        json.put("copyright", copyright);

        byte[] data = json.toString().getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, data.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(data);
        }
    }

    private void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        JSONObject json = new JSONObject();
        json.put("success", false);
        json.put("message", message);
        json.put("copyright", copyright);

        byte[] data = json.toString().getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, data.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(data);
        }
    }
}
