package team.kitemc.verifymc.web.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;
import team.kitemc.verifymc.core.PluginContext;
import team.kitemc.verifymc.web.WebResponseHelper;

import java.io.IOException;

/**
 * Returns the questionnaire configuration (questions, types, etc.).
 * Extracted from WebServer.start() — the "/api/questionnaire/config" context.
 */
public class QuestionnaireConfigHandler implements HttpHandler {
    private final PluginContext ctx;

    public QuestionnaireConfigHandler(PluginContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!WebResponseHelper.requireMethod(exchange, "GET")) return;

        String language = exchange.getRequestURI().getQuery();
        String lang = "en";
        if (language != null && language.contains("language=")) {
            lang = language.split("language=")[1].split("&")[0];
        }

        if (!ctx.getQuestionnaireService().isEnabled()) {
            JSONObject resp = new JSONObject();
            resp.put("success", false);
            resp.put("msg", "Questionnaire is not enabled");
            WebResponseHelper.sendJson(exchange, resp);
            return;
        }

        JSONObject config = ctx.getQuestionnaireService().getQuestionnaireConfig(lang);
        JSONObject resp = new JSONObject();
        resp.put("success", true);
        resp.put("config", config);
        WebResponseHelper.sendJson(exchange, resp);
    }
}
