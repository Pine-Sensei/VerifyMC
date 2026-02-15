package team.kitemc.verifymc.infrastructure.web;

import com.sun.net.httpserver.HttpExchange;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.HashMap;

public class RequestContext {
    private final HttpExchange exchange;
    private final Map<String, String> pathParams;
    private final Map<String, Object> attributes;
    private String requestBody;
    private String requestId;
    private int responseStatusCode = 200;
    private boolean halted = false;

    public RequestContext(HttpExchange exchange, Map<String, String> pathParams) {
        this.exchange = exchange;
        this.pathParams = pathParams != null ? pathParams : new HashMap<>();
        this.attributes = new HashMap<>();
        this.requestId = generateRequestId();
    }

    private String generateRequestId() {
        return String.format("%08x", System.currentTimeMillis() & 0xFFFFFFFF);
    }

    public String getRequestId() {
        return requestId;
    }

    public void status(int statusCode) {
        this.responseStatusCode = statusCode;
    }

    public int getStatus() {
        return responseStatusCode;
    }

    public HttpExchange getExchange() {
        return exchange;
    }

    public HttpExchange getRawExchange() {
        return exchange;
    }

    public String getMethod() {
        return exchange.getRequestMethod();
    }

    public String getPath() {
        return exchange.getRequestURI().getPath();
    }

    public String getQueryParam(String name) {
        String query = exchange.getRequestURI().getQuery();
        if (query == null) return null;
        
        for (String param : query.split("&")) {
            String[] pair = param.split("=", 2);
            if (pair[0].equals(name)) {
                return pair.length > 1 ? pair[1] : "";
            }
        }
        return null;
    }

    public String getQueryParam(String name, String defaultValue) {
        String value = getQueryParam(name);
        return value != null ? value : defaultValue;
    }

    public int getQueryParamAsInt(String name, int defaultValue) {
        String value = getQueryParam(name);
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public String getPathParam(String name) {
        return pathParams.get(name);
    }

    public Map<String, String> getPathParams() {
        return pathParams;
    }

    public String getHeader(String name) {
        return exchange.getRequestHeaders().getFirst(name);
    }

    public String getRequestBody() {
        if (requestBody == null) {
            try {
                requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            } catch (Exception e) {
                requestBody = "";
            }
        }
        return requestBody;
    }

    public JSONObject getBody() {
        try {
            String body = getRequestBody();
            if (body == null || body.isEmpty()) {
                return new JSONObject();
            }
            return new JSONObject(body);
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    public String getLanguage() {
        String lang = getQueryParam("language");
        if (lang != null && !lang.isEmpty()) {
            return lang;
        }
        String acceptLang = getHeader("Accept-Language");
        if (acceptLang != null && !acceptLang.isEmpty()) {
            return acceptLang.split(",")[0].split("-")[0];
        }
        return "en";
    }

    public void setAttribute(String name, Object value) {
        attributes.put(name, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String name) {
        return (T) attributes.get(name);
    }

    public String getClientIp() {
        String forwarded = getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        if (exchange.getRemoteAddress() != null && exchange.getRemoteAddress().getAddress() != null) {
            return exchange.getRemoteAddress().getAddress().getHostAddress();
        }
        return "unknown";
    }

    public void sendJson(JSONObject json) throws IOException {
        sendJson(200, json);
    }

    public void sendJson(int statusCode, JSONObject json) throws IOException {
        String response = json.toString();
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, response.getBytes(StandardCharsets.UTF_8).length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes(StandardCharsets.UTF_8));
        }
    }

    public void json(int status, Object data) throws IOException {
        if (data instanceof JSONObject) {
            sendJson(status, (JSONObject) data);
        } else if (data instanceof ApiResponse) {
            sendJson(status, ((ApiResponse) data).toJSONObject());
        } else {
            JSONObject json = new JSONObject();
            json.put("data", data);
            sendJson(status, json);
        }
    }

    public void sendUnauthorized() throws IOException {
        JSONObject resp = new JSONObject();
        resp.put("success", false);
        resp.put("code", "UNAUTHORIZED");
        resp.put("message", "Authentication required");
        sendJson(401, resp);
    }

    public void sendMethodNotAllowed() throws IOException {
        JSONObject resp = new JSONObject();
        resp.put("success", false);
        resp.put("code", "METHOD_NOT_ALLOWED");
        resp.put("message", "Method not allowed");
        sendJson(405, resp);
    }

    public void sendNotFound(String message) throws IOException {
        JSONObject resp = new JSONObject();
        resp.put("success", false);
        resp.put("code", "NOT_FOUND");
        resp.put("message", message != null ? message : "Resource not found");
        sendJson(404, resp);
    }

    public void sendError(int status, String message) throws IOException {
        JSONObject resp = new JSONObject();
        resp.put("success", false);
        resp.put("message", message);
        sendJson(status, resp);
    }

    public boolean isAuthenticated() {
        Object auth = getAttribute("auth.user");
        return auth != null;
    }

    public String getAuthToken() {
        return getAttribute("auth.token");
    }

    public void halt() {
        this.halted = true;
    }

    public boolean isHalted() {
        return halted;
    }
}
