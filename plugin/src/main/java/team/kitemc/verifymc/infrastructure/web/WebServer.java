package team.kitemc.verifymc.infrastructure.web;

import com.sun.net.httpserver.HttpServer;
import org.bukkit.plugin.Plugin;
import team.kitemc.verifymc.infrastructure.annotation.Service;
import team.kitemc.verifymc.infrastructure.lifecycle.Lifecycle;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class WebServer implements Lifecycle {
    private static final Logger LOGGER = Logger.getLogger(WebServer.class.getName());

    private final Plugin plugin;
    private final int port;
    private final String staticDir;
    private final HttpServerFactory serverFactory;
    private final StaticFileHandler staticFileHandler;
    private final ApiHandler apiHandler;
    private final Router router;
    private final WebAuthHelper authHelper;
    private final List<Object> controllers = new ArrayList<>();

    private HttpServer server;
    private LifecycleState state = LifecycleState.NEW;

    public WebServer(Plugin plugin, int port, String staticDir) {
        this.plugin = plugin;
        this.port = port;
        this.staticDir = staticDir;
        this.serverFactory = new HttpServerFactory();
        this.staticFileHandler = new StaticFileHandler(staticDir);
        this.router = new Router();
        this.apiHandler = new ApiHandler(router);
        this.authHelper = new WebAuthHelper(plugin);
    }

    @Override
    public void initialize() throws Exception {
        LOGGER.log(Level.INFO, "Initializing WebServer on port {0}", port);
        
        setupMiddlewares();
        registerControllers();
        
        state = LifecycleState.INITIALIZED;
    }

    @Override
    public void start() throws Exception {
        server = serverFactory.createDefault(port);

        server.createContext("/", staticFileHandler);
        server.createContext("/api", apiHandler);

        authHelper.startTokenCleanupTask();

        server.start();
        state = LifecycleState.STARTED;

        LOGGER.log(Level.INFO, "WebServer started on port {0}", port);
    }

    @Override
    public void stop() throws Exception {
        if (server != null) {
            server.stop(0);
            server = null;
        }
        state = LifecycleState.STOPPED;
        LOGGER.info("WebServer stopped");
    }

    @Override
    public String getName() {
        return "WebServer";
    }

    @Override
    public LifecycleState getState() {
        return state;
    }

    @Override
    public void setState(LifecycleState state) {
        this.state = state;
    }

    public void registerControllers() {
        for (Object controller : controllers) {
            registerController(controller);
        }
    }

    public void addController(Object controller) {
        controllers.add(controller);
    }

    private void registerController(Object controller) {
        LOGGER.log(Level.FINE, "Registering controller: {0}", controller.getClass().getSimpleName());
    }

    public void setupMiddlewares() {
        router.use(createAuthMiddleware());
        router.use(createLoggingMiddleware());
        router.use(createCorsMiddleware());
    }

    private Middleware createAuthMiddleware() {
        return context -> {
            String path = context.getPath();
            if (path.startsWith("/api/admin") || 
                path.equals("/api/reload-config") ||
                path.equals("/api/review") ||
                path.equals("/api/pending-list") ||
                path.equals("/api/all-users") ||
                path.equals("/api/users-paginated") ||
                path.startsWith("/api/delete-user") ||
                path.startsWith("/api/ban-user") ||
                path.startsWith("/api/unban-user") ||
                path.startsWith("/api/change-password")) {
                
                if (!authHelper.isAuthenticated(context.getExchange())) {
                    ApiResponse response = ApiResponse.error(
                        team.kitemc.verifymc.infrastructure.exception.ErrorCode.UNAUTHORIZED,
                        "Authentication required"
                    );
                    sendResponse(context, response, 401);
                    return false;
                }
            }
            return true;
        };
    }

    private Middleware createLoggingMiddleware() {
        return context -> {
            LOGGER.log(Level.FINE, "{0} {1} from {2}", 
                new Object[]{context.getMethod(), context.getPath(), context.getClientIp()});
            return true;
        };
    }

    private Middleware createCorsMiddleware() {
        return context -> {
            context.getExchange().getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            context.getExchange().getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            context.getExchange().getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
            
            if ("OPTIONS".equals(context.getMethod())) {
                sendResponse(context, ApiResponse.success(), 200);
                return false;
            }
            return true;
        };
    }

    private void sendResponse(RequestContext context, ApiResponse response, int statusCode) {
        try {
            org.json.JSONObject json = response.toJSONObject();
            json.put("copyright", "Powered by VerifyMC (GPLv3)");
            
            byte[] data = json.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
            context.getExchange().getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            context.getExchange().sendResponseHeaders(statusCode, data.length);
            context.getExchange().getResponseBody().write(data);
            context.getExchange().close();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error sending response", e);
        }
    }

    public Router getRouter() {
        return router;
    }

    public WebAuthHelper getAuthHelper() {
        return authHelper;
    }

    public void updateStaticDir(String newDir) {
        staticFileHandler.setBaseDir(newDir);
    }
}
