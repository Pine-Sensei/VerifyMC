package team.kitemc.verifymc.infrastructure.web;

import com.sun.net.httpserver.HttpExchange;
import java.util.Map;
import java.util.HashMap;

public class RequestContext {
    private final HttpExchange exchange;
    private final Map<String, String> pathParams;
    private final Map<String, Object> attributes;
    private String requestBody;

    public RequestContext(HttpExchange exchange, Map<String, String> pathParams) {
        this.exchange = exchange;
        this.pathParams = pathParams != null ? pathParams : new HashMap<>();
        this.attributes = new HashMap<>();
    }

    public HttpExchange getExchange() {
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
                requestBody = new String(exchange.getRequestBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            } catch (Exception e) {
                requestBody = "";
            }
        }
        return requestBody;
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
}
