package team.kitemc.verifymc.infrastructure.web;

import org.json.JSONObject;
import team.kitemc.verifymc.infrastructure.exception.BusinessException;
import team.kitemc.verifymc.infrastructure.exception.ErrorCode;
import team.kitemc.verifymc.infrastructure.exception.ValidationException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApiResponse {
    private final boolean success;
    private final String code;
    private final String message;
    private final Object data;
    private final long timestamp;

    public ApiResponse(boolean success, String code, String message, Object data) {
        this.success = success;
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    public boolean isSuccess() {
        return success;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public Object getData() {
        return data;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public static ApiResponse success() {
        return new ApiResponse(true, "SUCCESS", "Success", null);
    }

    public static ApiResponse success(String message) {
        return new ApiResponse(true, "SUCCESS", message, null);
    }

    public static ApiResponse success(Object data) {
        return new ApiResponse(true, "SUCCESS", "Success", data);
    }

    public static ApiResponse success(String message, Object data) {
        return new ApiResponse(true, "SUCCESS", message, data);
    }

    public static ApiResponse error(ErrorCode errorCode, String message) {
        return new ApiResponse(false, errorCode.getCode(), message, null);
    }

    public static ApiResponse error(ErrorCode errorCode) {
        return new ApiResponse(false, errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static ApiResponse error(BusinessException exception) {
        return new ApiResponse(false, exception.getErrorCodeString(), exception.getMessage(), null);
    }

    public static ApiResponse error(BusinessException exception, Object details) {
        return new ApiResponse(false, exception.getErrorCodeString(), exception.getMessage(), details);
    }

    public static ApiResponse validationError(List<ValidationException.FieldError> fieldErrors) {
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("fieldErrors", fieldErrors);
        return new ApiResponse(false, ErrorCode.VALIDATION_ERROR.getCode(), 
            ErrorCode.VALIDATION_ERROR.getMessage(), errorDetails);
    }

    public JSONObject toJSONObject() {
        JSONObject json = new JSONObject();
        json.put("success", success);
        json.put("code", code);
        json.put("message", message);
        json.put("timestamp", timestamp);
        if (data != null) {
            json.put("data", data);
        }
        return json;
    }

    @Override
    public String toString() {
        return toJSONObject().toString();
    }
}
