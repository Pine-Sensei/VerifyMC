package team.kitemc.verifymc.web.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;
import team.kitemc.verifymc.core.PluginContext;
import team.kitemc.verifymc.web.WebResponseHelper;

import java.io.IOException;

/**
 * Returns the current plugin version and checks for updates.
 */
public class VersionHandler implements HttpHandler {
    private final PluginContext ctx;

    public VersionHandler(PluginContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!WebResponseHelper.requireMethod(exchange, "GET")) return;

        JSONObject resp = new JSONObject();
        resp.put("success", true);
        resp.put("version", ctx.getPlugin().getDescription().getVersion());

        if (ctx.getVersionCheckService() != null) {
            JSONObject info = ctx.getVersionCheckService().getVersionInfo();
            if (info != null) {
                resp.put("latest_version", info.optString("latestVersion", ""));
                resp.put("update_available", info.optBoolean("updateAvailable", false));
                resp.put("download_url", info.optString("releasesUrl", ""));
            }
        }

        WebResponseHelper.sendJson(exchange, resp);
    }
}
