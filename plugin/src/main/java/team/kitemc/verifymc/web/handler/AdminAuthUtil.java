package team.kitemc.verifymc.web.handler;

import com.sun.net.httpserver.HttpExchange;
import team.kitemc.verifymc.core.PluginContext;
import team.kitemc.verifymc.web.ApiResponseFactory;
import team.kitemc.verifymc.web.WebResponseHelper;

import java.io.IOException;

/**
 * Shared authentication utility for admin handlers.
 * Extracts the Bearer token from the Authorization header and validates it.
 */
public final class AdminAuthUtil {
    private AdminAuthUtil() {}

    /**
     * Validates the Authorization header. Returns true if the token is valid.
     * Sends a 401 response and returns false if not.
     */
    public static boolean requireAuth(HttpExchange exchange, PluginContext ctx) throws IOException {
        String token = exchange.getRequestHeaders().getFirst("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        if (token == null || !ctx.getWebAuthHelper().isValidToken(token)) {
            WebResponseHelper.sendJson(exchange, ApiResponseFactory.failure("Unauthorized"), 401);
            return false;
        }
        return true;
    }
}
