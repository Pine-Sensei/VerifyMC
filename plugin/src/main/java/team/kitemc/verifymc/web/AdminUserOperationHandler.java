package team.kitemc.verifymc.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.util.function.BiFunction;
import org.bukkit.plugin.Plugin;
import org.json.JSONObject;

public class AdminUserOperationHandler {
    private final Plugin plugin;
    private final WebAuthHelper authHelper;
    private final BiFunction<String, String, String> messageResolver;

    public AdminUserOperationHandler(Plugin plugin, WebAuthHelper authHelper, BiFunction<String, String, String> messageResolver) {
        this.plugin = plugin;
        this.authHelper = authHelper;
        this.messageResolver = messageResolver;
    }

    public HttpHandler adminLoginHandler() {
        return exchange -> {
            if (!WebResponseHelper.requireMethod(exchange, "POST")) {
                return;
            }
            JSONObject req = WebResponseHelper.readJson(exchange);
            String password = req.optString("password");
            String language = req.optString("language", "en");

            String adminPassword = plugin.getConfig().getString("admin.password", "");
            JSONObject resp = new JSONObject();
            if (password.equals(adminPassword)) {
                resp.put("success", true);
                resp.put("token", authHelper.generateSecureToken());
                resp.put("message", messageResolver.apply("admin.login_success", language));
            } else {
                resp.put("success", false);
                resp.put("message", messageResolver.apply("admin.login_failed", language));
            }
            WebResponseHelper.sendJson(exchange, resp);
        };
    }

    public HttpHandler adminVerifyHandler() {
        return exchange -> {
            if (!WebResponseHelper.requireMethod(exchange, "POST")) {
                return;
            }
            JSONObject resp = new JSONObject();
            if (authHelper.isAuthenticated(exchange)) {
                resp.put("success", true);
                resp.put("message", "Token is valid");
            } else {
                resp.put("success", false);
                resp.put("message", "Invalid or expired token");
            }
            WebResponseHelper.sendJson(exchange, resp);
        };
    }
}
