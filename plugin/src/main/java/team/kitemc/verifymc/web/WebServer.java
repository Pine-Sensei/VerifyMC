package team.kitemc.verifymc.web;

import com.sun.net.httpserver.HttpServer;
import team.kitemc.verifymc.core.PluginContext;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * Lightweight web server wrapper. Reduced from 1862 lines to ~80 lines.
 * <p>
 * All routing logic is delegated to {@link ApiRouter}. This class is
 * responsible only for server lifecycle: start, stop, and thread pool setup.
 */
public class WebServer {
    private final PluginContext ctx;
    private HttpServer server;
    private final ApiRouter router;

    public WebServer(PluginContext ctx) {
        this.ctx = ctx;
        this.router = new ApiRouter(ctx);
    }

    /**
     * Start the HTTP server on the configured port.
     */
    public void start() {
        int port = ctx.getConfigManager().getWebPort();
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.setExecutor(Executors.newFixedThreadPool(
                    Runtime.getRuntime().availableProcessors() * 2));

            // Register all API routes via the router
            router.registerRoutes(server);

            server.start();
            ctx.getPlugin().getLogger().info("[VerifyMC] Web server started on port " + port);
        } catch (IOException e) {
            ctx.getPlugin().getLogger().severe("[VerifyMC] Failed to start web server: " + e.getMessage());
        }
    }

    /**
     * Stop the HTTP server gracefully.
     */
    public void stop() {
        if (server != null) {
            server.stop(0);
            ctx.getPlugin().getLogger().info("[VerifyMC] Web server stopped.");
        }
    }

    /**
     * Get the underlying HTTP server instance (for tests or advanced usage).
     */
    public HttpServer getServer() {
        return server;
    }
}
