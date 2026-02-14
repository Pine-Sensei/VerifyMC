package team.kitemc.verifymc.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;

public class RegistrationHandler implements HttpHandler {
    private final HttpHandler delegate;

    public RegistrationHandler(HttpHandler delegate) {
        this.delegate = delegate;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        delegate.handle(exchange);
    }
}
