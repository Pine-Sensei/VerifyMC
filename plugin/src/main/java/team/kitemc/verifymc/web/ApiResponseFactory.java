package team.kitemc.verifymc.web;

import org.json.JSONObject;

public final class ApiResponseFactory {
    private ApiResponseFactory() {
    }

    public static JSONObject success(String message) {
        return create(true, message);
    }

    public static JSONObject failure(String message) {
        return create(false, message);
    }

    public static JSONObject create(boolean success, String message) {
        JSONObject response = new JSONObject();
        response.put("success", success);
        response.put("msg", message);
        response.put("message", message);
        return response;
    }
}
