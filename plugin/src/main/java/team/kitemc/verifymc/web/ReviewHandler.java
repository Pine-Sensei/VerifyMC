package team.kitemc.verifymc.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;

/**
 * Delegate handler for review-related endpoints.
 * Serves as an extension point for future cross-cutting concerns (e.g. rate limiting, logging, CORS).
 */
public class ReviewHandler implements HttpHandler {
    private final HttpHandler delegate;

    public ReviewHandler(HttpHandler delegate) {
        this.delegate = delegate;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        delegate.handle(exchange);
    }
}
