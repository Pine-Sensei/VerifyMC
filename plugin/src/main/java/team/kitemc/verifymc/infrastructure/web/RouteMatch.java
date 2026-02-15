package team.kitemc.verifymc.infrastructure.web;

import java.util.Map;

public class RouteMatch {
    private final RouteHandler handler;
    private final Map<String, String> params;

    public RouteMatch(RouteHandler handler, Map<String, String> params) {
        this.handler = handler;
        this.params = params;
    }

    public RouteHandler getHandler() {
        return handler;
    }

    public Map<String, String> getParams() {
        return params;
    }
}
