package team.kitemc.verifymc.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;

/**
 * Delegate handler for user administration endpoints.
 * Serves as an extension point for future cross-cutting concerns (e.g. rate limiting, logging, CORS).
 */
public class UserAdminHandler implements HttpHandler {
    private final HttpHandler delegate;

    public UserAdminHandler(HttpHandler delegate) {
        this.delegate = delegate;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        delegate.handle(exchange);
    }
}
