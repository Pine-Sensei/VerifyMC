package team.kitemc.verifymc.web.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import team.kitemc.verifymc.core.PluginContext;
import team.kitemc.verifymc.service.DiscordService;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Handles Discord OAuth2 callback — exchanges code for token, links the account.
 */
public class DiscordCallbackHandler implements HttpHandler {
    private final PluginContext ctx;

    public DiscordCallbackHandler(PluginContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        String code = null;
        String state = null;
        if (query != null) {
            for (String param : query.split("&")) {
                String[] kv = param.split("=", 2);
                if (kv.length == 2) {
                    if ("code".equals(kv[0])) code = kv[1];
                    if ("state".equals(kv[0])) state = kv[1];
                }
            }
        }

        String html;
        if (code == null || state == null) {
            html = buildCallbackHtml(false, "Missing code or state parameter");
        } else {
            DiscordService.DiscordCallbackResult result = ctx.getDiscordService().handleCallback(code, state);
            boolean success = result.success;
            html = buildCallbackHtml(success, success ? "Discord linked successfully!" : result.message);
        }

        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String buildCallbackHtml(boolean success, String message) {
        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"><title>Discord Link</title>
            <style>
                body { font-family: Arial, sans-serif; display: flex; justify-content: center; align-items: center; height: 100vh; margin: 0; background: #36393f; color: #fff; }
                .container { text-align: center; padding: 40px; background: #2f3136; border-radius: 8px; }
                .success { color: #43b581; }
                .error { color: #f04747; }
            </style>
            </head>
            <body>
                <div class="container">
                    <h2 class="%s">%s</h2>
                    <p>%s</p>
                    <p>You can close this window now.</p>
                </div>
            </body>
            </html>
            """.formatted(
                success ? "success" : "error",
                success ? "✓ Success" : "✗ Error",
                message
            );
    }
}
