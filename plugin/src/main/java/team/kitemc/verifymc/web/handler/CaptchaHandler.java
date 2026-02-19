package team.kitemc.verifymc.web.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;
import team.kitemc.verifymc.core.PluginContext;
import team.kitemc.verifymc.web.WebResponseHelper;

import java.io.IOException;

/**
 * Generates a captcha challenge (math or text).
 * Extracted from WebServer.start() — the "/api/captcha/generate" context.
 */
public class CaptchaHandler implements HttpHandler {
    private final PluginContext ctx;

    public CaptchaHandler(PluginContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!WebResponseHelper.requireMethod(exchange, "GET")) return;

        try {
            var result = ctx.getCaptchaService().generateCaptcha();
            JSONObject resp = new JSONObject();
            resp.put("success", true);
            resp.put("token", result.token());
            resp.put("image", result.imageBase64());
            if (ctx.getConfigManager().isDebug()) {
                resp.put("answer", result.answer());
            }
            WebResponseHelper.sendJson(exchange, resp);
        } catch (Exception e) {
            JSONObject resp = new JSONObject();
            resp.put("success", false);
            resp.put("msg", "Failed to generate captcha");
            WebResponseHelper.sendJson(exchange, resp, 500);
        }
    }
}
