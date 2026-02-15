package team.kitemc.verifymc.infrastructure.web;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class CorsMiddleware implements Middleware {
    private final Set<String> allowedOrigins;
    private final Set<String> allowedMethods;
    private final Set<String> allowedHeaders;
    private final boolean allowCredentials;
    private final int maxAge;

    public CorsMiddleware() {
        this("*", true);
    }

    public CorsMiddleware(String allowedOrigin, boolean allowCredentials) {
        this(Arrays.asList(allowedOrigin), allowCredentials);
    }

    public CorsMiddleware(Iterable<String> allowedOrigins, boolean allowCredentials) {
        this.allowedOrigins = new HashSet<>();
        for (String origin : allowedOrigins) {
            this.allowedOrigins.add(origin);
        }
        this.allowedMethods = new HashSet<>(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        this.allowedHeaders = new HashSet<>(Arrays.asList("Content-Type", "Authorization", "X-Requested-With", "Accept", "Origin"));
        this.allowCredentials = allowCredentials;
        this.maxAge = 3600;
    }

    public CorsMiddleware withMethods(String... methods) {
        this.allowedMethods.clear();
        this.allowedMethods.addAll(Arrays.asList(methods));
        return this;
    }

    public CorsMiddleware withHeaders(String... headers) {
        this.allowedHeaders.clear();
        this.allowedHeaders.addAll(Arrays.asList(headers));
        return this;
    }

    @Override
    public void handle(RequestContext ctx, MiddlewareChain next) throws Exception {
        String origin = ctx.getHeader("Origin");

        if ("OPTIONS".equals(ctx.getMethod())) {
            handlePreflight(ctx, origin);
            return;
        }

        setCorsHeaders(ctx, origin);
        next.next();
    }

    private void handlePreflight(RequestContext ctx, String origin) throws Exception {
        setCorsHeaders(ctx, origin);

        String requestMethod = ctx.getHeader("Access-Control-Request-Method");
        if (requestMethod != null && allowedMethods.contains(requestMethod)) {
            ctx.getRawExchange().getResponseHeaders().set("Access-Control-Allow-Methods", String.join(", ", allowedMethods));
        }

        String requestHeaders = ctx.getHeader("Access-Control-Request-Headers");
        if (requestHeaders != null) {
            ctx.getRawExchange().getResponseHeaders().set("Access-Control-Allow-Headers", String.join(", ", allowedHeaders));
        }

        ctx.getRawExchange().getResponseHeaders().set("Access-Control-Max-Age", String.valueOf(maxAge));
        ctx.status(204);
    }

    private void setCorsHeaders(RequestContext ctx, String origin) {
        if (origin != null && isOriginAllowed(origin)) {
            ctx.getRawExchange().getResponseHeaders().set("Access-Control-Allow-Origin", origin);
        } else if (allowedOrigins.contains("*")) {
            ctx.getRawExchange().getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        }

        if (allowCredentials) {
            ctx.getRawExchange().getResponseHeaders().set("Access-Control-Allow-Credentials", "true");
        }

        ctx.getRawExchange().getResponseHeaders().set("Access-Control-Expose-Headers", "Content-Type, Authorization");
    }

    private boolean isOriginAllowed(String origin) {
        if (allowedOrigins.contains("*")) {
            return true;
        }
        return allowedOrigins.contains(origin);
    }
}
