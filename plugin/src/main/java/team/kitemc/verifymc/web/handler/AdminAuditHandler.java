package team.kitemc.verifymc.web.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONArray;
import org.json.JSONObject;
import team.kitemc.verifymc.core.PluginContext;
import team.kitemc.verifymc.db.AuditRecord;
import team.kitemc.verifymc.web.ApiResponseFactory;
import team.kitemc.verifymc.web.WebResponseHelper;

import java.io.IOException;
import java.util.List;

/**
 * Returns the audit log.
 */
public class AdminAuditHandler implements HttpHandler {
    private final PluginContext ctx;

    public AdminAuditHandler(PluginContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!WebResponseHelper.requireMethod(exchange, "GET")) return;

        // Require admin privileges
        if (AdminAuthUtil.requireAdmin(exchange, ctx) == null) return;

        List<AuditRecord> audits = ctx.getAuditDao().getAllAudits();
        JSONArray arr = new JSONArray();
        for (AuditRecord audit : audits) {
            JSONObject obj = new JSONObject();
            if (audit.id() != null) obj.put("id", audit.id());
            obj.put("action", audit.action());
            obj.put("operator", audit.operator());
            obj.put("target", audit.target());
            obj.put("detail", audit.detail());
            obj.put("timestamp", audit.timestamp());
            arr.put(obj);
        }

        JSONObject resp = new JSONObject();
        resp.put("success", true);
        resp.put("audits", arr);
        WebResponseHelper.sendJson(exchange, resp);
    }
}
