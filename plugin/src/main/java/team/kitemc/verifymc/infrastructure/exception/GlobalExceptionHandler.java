package team.kitemc.verifymc.infrastructure.exception;

import org.json.JSONObject;
import team.kitemc.verifymc.infrastructure.web.ApiResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class GlobalExceptionHandler {
    private static final Logger LOGGER = Logger.getLogger(GlobalExceptionHandler.class.getName());
    private static final boolean DEBUG = Boolean.getBoolean("verifymc.debug");

    private GlobalExceptionHandler() {
    }

    public static ApiResponse handle(Exception exception) {
        if (exception instanceof ValidationException) {
            return handleValidationException((ValidationException) exception);
        }
        if (exception instanceof BusinessException) {
            return handleBusinessException((BusinessException) exception);
        }
        return handleUnexpectedException(exception);
    }

    public static JSONObject handleToJson(Exception exception) {
        return handle(exception).toJSONObject();
    }

    private static ApiResponse handleBusinessException(BusinessException exception) {
        LOGGER.log(Level.WARNING, "Business exception occurred: {0} - {1}", 
            new Object[]{exception.getErrorCodeString(), exception.getMessage()});
        
        if (exception.getDetails() != null) {
            return ApiResponse.error(exception, exception.getDetails());
        }
        return ApiResponse.error(exception);
    }

    private static ApiResponse handleValidationException(ValidationException exception) {
        LOGGER.log(Level.FINE, "Validation exception: {0}", exception.getMessage());
        
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("fieldErrors", exception.getFieldErrors());
        return new ApiResponse(false, ErrorCode.VALIDATION_ERROR.getCode(),
            exception.getMessage(), errorDetails);
    }

    private static ApiResponse handleUnexpectedException(Exception exception) {
        LOGGER.log(Level.SEVERE, "Unexpected exception occurred", exception);
        
        if (DEBUG) {
            Map<String, Object> errorDetails = new HashMap<>();
            errorDetails.put("exceptionType", exception.getClass().getName());
            errorDetails.put("exceptionMessage", exception.getMessage());
            return new ApiResponse(false, ErrorCode.INTERNAL_ERROR.getCode(),
                "Internal server error: " + exception.getMessage(), errorDetails);
        }
        
        return ApiResponse.error(ErrorCode.INTERNAL_ERROR, "An unexpected error occurred");
    }

    public static int getHttpStatus(Exception exception) {
        if (exception instanceof BusinessException) {
            return ((BusinessException) exception).getHttpStatus();
        }
        return 500;
    }

    public static String getSafeErrorMessage(Exception exception) {
        if (exception instanceof BusinessException) {
            return exception.getMessage();
        }
        if (DEBUG) {
            return exception.getMessage();
        }
        return "An unexpected error occurred";
    }
}
