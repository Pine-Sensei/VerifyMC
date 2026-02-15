package team.kitemc.verifymc.web;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ExceptionHandler {
    private static final Logger LOGGER = Logger.getLogger(ExceptionHandler.class.getName());
    
    public static void handleException(HttpExchange exchange, Exception e) {
        try {
            ApiResponse response;
            int statusCode;
            
            if (e instanceof BusinessException) {
                BusinessException be = (BusinessException) e;
                response = ApiResponse.failure(be.getMessage(), be.getCode());
                statusCode = 400;
                LOGGER.log(Level.WARNING, "Business exception: " + be.getMessage(), be);
            } else {
                response = ApiResponse.failure("Internal server error", ErrorCode.INTERNAL_ERROR.getCode());
                statusCode = 500;
                LOGGER.log(Level.SEVERE, "Unexpected exception: " + e.getMessage(), e);
            }
            
            String json = response.toJson();
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(statusCode, json.getBytes(StandardCharsets.UTF_8).length);
            
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }
        } catch (IOException ioe) {
            LOGGER.log(Level.SEVERE, "Failed to send error response", ioe);
        }
    }
    
    public static String toJson(Exception e) {
        if (e instanceof BusinessException) {
            BusinessException be = (BusinessException) e;
            return ApiResponse.failure(be.getMessage(), be.getCode()).toJson();
        }
        return ApiResponse.failure("Internal server error", ErrorCode.INTERNAL_ERROR.getCode()).toJson();
    }
}
