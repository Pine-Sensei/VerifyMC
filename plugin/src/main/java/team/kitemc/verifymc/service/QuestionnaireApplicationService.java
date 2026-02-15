package team.kitemc.verifymc.service;

import org.json.JSONObject;
import team.kitemc.verifymc.web.ApiResponseFactory;

public class QuestionnaireApplicationService {
    public JSONObject buildAnswersRequiredResponse(String message) {
        return ApiResponseFactory.failure(message);
    }
}
