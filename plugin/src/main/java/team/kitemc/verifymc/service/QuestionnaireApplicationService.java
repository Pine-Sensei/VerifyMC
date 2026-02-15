package team.kitemc.verifymc.service;

import org.json.JSONObject;
import team.kitemc.verifymc.web.ApiResponseFactory;

public class QuestionnaireApplicationService implements IQuestionnaireApplicationService {
    @Override
    public JSONObject buildAnswersRequiredResponse(String message) {
        return ApiResponseFactory.failure(message);
    }
}
