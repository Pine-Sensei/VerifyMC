package team.kitemc.verifymc.web.router;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class ApiRouter {
    private static final Logger LOGGER = Logger.getLogger(ApiRouter.class.getName());
    private final HttpServer server;
    private final Map<String, HttpHandler> routes = new HashMap<>();
    
    public ApiRouter(HttpServer server) {
        this.server = server;
    }
    
    public ApiRouter get(String path, HttpHandler handler) {
        routes.put("GET:" + path, handler);
        server.createContext(path, handler);
        LOGGER.info("Registered GET route: " + path);
        return this;
    }
    
    public ApiRouter post(String path, HttpHandler handler) {
        routes.put("POST:" + path, handler);
        server.createContext(path, handler);
        LOGGER.info("Registered POST route: " + path);
        return this;
    }
    
    public ApiRouter all(String path, HttpHandler handler) {
        routes.put("ALL:" + path, handler);
        server.createContext(path, handler);
        LOGGER.info("Registered route: " + path);
        return this;
    }
    
    public HttpHandler getHandler(String method, String path) {
        HttpHandler handler = routes.get(method + ":" + path);
        if (handler == null) {
            handler = routes.get("ALL:" + path);
        }
        return handler;
    }
    
    public Map<String, HttpHandler> getRoutes() {
        return new HashMap<>(routes);
    }
}
