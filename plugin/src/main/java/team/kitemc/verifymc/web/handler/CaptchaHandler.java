package team.kitemc.verifymc.web.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;
import team.kitemc.verifymc.core.PluginContext;
import team.kitemc.verifymc.web.WebResponseHelper;

import java.io.IOException;

public class CaptchaHandler implements HttpHandler {
    private final PluginContext ctx;

    public CaptchaHandler(PluginContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!WebResponseHelper.requireMethod(exchange, "GET")) return;

        String query = exchange.getRequestURI().getQuery();
        String language = "en";
        if (query != null) {
            for (String param : query.split("&")) {
                String[] kv = param.split("=", 2);
                if (kv.length == 2 && "language".equals(kv[0])) {
                    language = kv[1];
                }
            }
        }

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
            resp.put("msg", ctx.getMessage("captcha.generate_failed", language));
            WebResponseHelper.sendJson(exchange, resp, 500);
        }
    }
}
