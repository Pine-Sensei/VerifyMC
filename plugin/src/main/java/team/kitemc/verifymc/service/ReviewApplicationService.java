package team.kitemc.verifymc.service;

import java.util.function.Function;
import org.json.JSONObject;
import team.kitemc.verifymc.web.ApiResponseFactory;

public class ReviewApplicationService {
    public JSONObject buildReviewResponse(boolean reviewSuccess, boolean approve, Function<String, String> messageResolver) {
        if (!reviewSuccess) {
            return ApiResponseFactory.failure(messageResolver.apply("review.failed"));
        }
        String key = approve ? "review.approve_success" : "review.reject_success";
        return ApiResponseFactory.success(messageResolver.apply(key));
    }
}
