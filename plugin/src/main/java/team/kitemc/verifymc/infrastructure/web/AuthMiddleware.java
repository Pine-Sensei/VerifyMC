package team.kitemc.verifymc.infrastructure.web;

import org.json.JSONObject;

import java.util.function.Function;

public class AuthMiddleware implements Middleware {
    private final TokenExtractor tokenExtractor;
    private final TokenValidator tokenValidator;
    private final String realm;

    public AuthMiddleware(TokenExtractor tokenExtractor, TokenValidator tokenValidator) {
        this(tokenExtractor, tokenValidator, "VerifyMC API");
    }

    public AuthMiddleware(TokenExtractor tokenExtractor, TokenValidator tokenValidator, String realm) {
        this.tokenExtractor = tokenExtractor;
        this.tokenValidator = tokenValidator;
        this.realm = realm;
    }

    @Override
    public void handle(RequestContext ctx, MiddlewareChain next) throws Exception {
        String token = tokenExtractor.extract(ctx);

        if (token == null || token.isEmpty()) {
            sendUnauthorized(ctx, "Authentication required");
            return;
        }

        AuthResult result = tokenValidator.validate(token);
        if (!result.isValid()) {
            sendUnauthorized(ctx, result.getErrorMessage());
            return;
        }

        ctx.setAttribute("auth.user", result.getUser());
        ctx.setAttribute("auth.token", token);

        next.next();
    }

    private void sendUnauthorized(RequestContext ctx, String message) throws Exception {
        ctx.getRawExchange().getResponseHeaders().set("WWW-Authenticate", "Bearer realm=\"" + realm + "\"");

        JSONObject errorResponse = new JSONObject();
        errorResponse.put("success", false);
        errorResponse.put("error", "unauthorized");
        errorResponse.put("message", message);

        ctx.json(401, errorResponse);
    }

    @FunctionalInterface
    public interface TokenExtractor {
        String extract(RequestContext ctx);
    }

    @FunctionalInterface
    public interface TokenValidator {
        AuthResult validate(String token);
    }

    public static class AuthResult {
        private final boolean valid;
        private final String errorMessage;
        private final Object user;

        private AuthResult(boolean valid, String errorMessage, Object user) {
            this.valid = valid;
            this.errorMessage = errorMessage;
            this.user = user;
        }

        public static AuthResult success(Object user) {
            return new AuthResult(true, null, user);
        }

        public static AuthResult success() {
            return new AuthResult(true, null, null);
        }

        public static AuthResult failure(String errorMessage) {
            return new AuthResult(false, errorMessage, null);
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        @SuppressWarnings("unchecked")
        public <T> T getUser() {
            return (T) user;
        }
    }

    public static TokenExtractor bearerToken() {
        return ctx -> {
            String authHeader = ctx.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                return authHeader.substring(7);
            }
            return null;
        };
    }

    public static TokenExtractor headerToken(String headerName) {
        return ctx -> ctx.getHeader(headerName);
    }

    public static TokenExtractor queryParamToken(String paramName) {
        return ctx -> ctx.getQueryParam(paramName);
    }

    public static TokenValidator staticTokenValidator(String validToken) {
        return token -> {
            if (validToken.equals(token)) {
                return AuthResult.success();
            }
            return AuthResult.failure("Invalid token");
        };
    }

    public static TokenValidator functionalValidator(Function<String, Boolean> validator) {
        return token -> {
            if (validator.apply(token)) {
                return AuthResult.success();
            }
            return AuthResult.failure("Invalid token");
        };
    }
}
