package team.kitemc.verifymc.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ApiResponse {
    private static final Gson GSON = new GsonBuilder().create();
    
    private final boolean success;
    private final String msg;
    private final Object data;
    private final String code;
    
    private ApiResponse(boolean success, String msg, Object data, String code) {
        this.success = success;
        this.msg = msg;
        this.data = data;
        this.code = code;
    }
    
    public static ApiResponse success(String msg) {
        return new ApiResponse(true, msg, null, null);
    }
    
    public static ApiResponse success(String msg, Object data) {
        return new ApiResponse(true, msg, data, null);
    }
    
    public static ApiResponse failure(String msg) {
        return new ApiResponse(false, msg, null, null);
    }
    
    public static ApiResponse failure(String msg, String code) {
        return new ApiResponse(false, msg, null, code);
    }
    
    public String toJson() {
        return GSON.toJson(this);
    }
    
    public boolean isSuccess() { return success; }
    public String getMsg() { return msg; }
    public Object getData() { return data; }
    public String getCode() { return code; }
}
