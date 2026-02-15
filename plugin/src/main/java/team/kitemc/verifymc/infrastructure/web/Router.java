package team.kitemc.verifymc.infrastructure.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Router {
    private final List<RouteEntry> routes = new ArrayList<>();
    private final List<Middleware> middlewares = new ArrayList<>();
    private final String prefix;

    public Router() {
        this("");
    }

    public Router(String prefix) {
        this.prefix = prefix != null ? prefix : "";
    }

    public Router get(String path, RouteHandler handler) {
        routes.add(new RouteEntry("GET", normalizePath(prefix + path), handler));
        return this;
    }

    public Router post(String path, RouteHandler handler) {
        routes.add(new RouteEntry("POST", normalizePath(prefix + path), handler));
        return this;
    }

    public Router put(String path, RouteHandler handler) {
        routes.add(new RouteEntry("PUT", normalizePath(prefix + path), handler));
        return this;
    }

    public Router delete(String path, RouteHandler handler) {
        routes.add(new RouteEntry("DELETE", normalizePath(prefix + path), handler));
        return this;
    }

    public Router any(String path, RouteHandler handler) {
        routes.add(new RouteEntry("*", normalizePath(prefix + path), handler));
        return this;
    }

    public void group(String groupPrefix, Consumer<Router> groupConfigurer) {
        String fullPrefix = normalizePath(this.prefix + groupPrefix);
        Router groupRouter = new Router(fullPrefix);
        groupRouter.middlewares.addAll(this.middlewares);
        groupConfigurer.accept(groupRouter);
        this.routes.addAll(groupRouter.routes);
    }

    public Router use(Middleware middleware) {
        middlewares.add(middleware);
        return this;
    }

    public List<Middleware> getMiddlewares() {
        return middlewares;
    }

    private String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return "/";
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        while (path.contains("//")) {
            path = path.replace("//", "/");
        }
        if (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    public RouteMatch match(String method, String path) {
        for (RouteEntry entry : routes) {
            RouteMatch match = entry.match(method, path);
            if (match != null) {
                return wrapWithMiddleware(match);
            }
        }
        return null;
    }

    private RouteMatch wrapWithMiddleware(RouteMatch match) {
        if (middlewares.isEmpty()) {
            return match;
        }
        RouteHandler wrappedHandler = ctx -> {
            List<Middleware> middlewareList = new ArrayList<>(middlewares);
            executeChain(ctx, middlewareList, 0, match.handler());
        };
        return new RouteMatch(wrappedHandler, match.pathParams());
    }

    private void executeChain(RequestContext ctx, List<Middleware> middlewareList, int index, RouteHandler finalHandler) throws Exception {
        if (index >= middlewareList.size()) {
            finalHandler.handle(ctx);
            return;
        }
        Middleware middleware = middlewareList.get(index);
        middleware.handle(ctx, () -> executeChain(ctx, middlewareList, index + 1, finalHandler));
    }

    private static class RouteEntry {
        private final String method;
        private final String pathPattern;
        private final RouteHandler handler;
        private final Pattern compiledPattern;
        private final List<String> paramNames;

        RouteEntry(String method, String pathPattern, RouteHandler handler) {
            this.method = method;
            this.pathPattern = pathPattern;
            this.handler = handler;
            this.paramNames = new ArrayList<>();
            this.compiledPattern = compilePattern(pathPattern);
        }

        private Pattern compilePattern(String path) {
            StringBuilder regex = new StringBuilder("^");
            String[] parts = path.split("/");
            
            for (String part : parts) {
                if (part.isEmpty()) continue;
                
                regex.append("/");
                if (part.startsWith(":")) {
                    String paramName = part.substring(1);
                    if (paramName.endsWith("?")) {
                        paramName = paramName.substring(0, paramName.length() - 1);
                        regex.append("([^/]*)");
                    } else {
                        regex.append("([^/]+)");
                    }
                    paramNames.add(paramName);
                } else if (part.startsWith("{") && part.endsWith("}")) {
                    String paramName = part.substring(1, part.length() - 1);
                    regex.append("([^/]+)");
                    paramNames.add(paramName);
                } else if (part.equals("*")) {
                    regex.append(".*");
                } else {
                    regex.append(Pattern.quote(part));
                }
            }
            
            regex.append("/?$");
            return Pattern.compile(regex.toString());
        }

        RouteMatch match(String requestMethod, String requestPath) {
            if (!method.equals("*") && !method.equalsIgnoreCase(requestMethod)) {
                return null;
            }

            Matcher matcher = compiledPattern.matcher(requestPath);
            if (!matcher.matches()) {
                return null;
            }

            Map<String, String> params = new HashMap<>();
            for (int i = 0; i < paramNames.size() && i < matcher.groupCount(); i++) {
                String value = matcher.group(i + 1);
                if (value != null && !value.isEmpty()) {
                    params.put(paramNames.get(i), value);
                }
            }

            return new RouteMatch(handler, params);
        }
    }
}
