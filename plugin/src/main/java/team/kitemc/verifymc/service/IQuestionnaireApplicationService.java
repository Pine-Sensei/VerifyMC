package team.kitemc.verifymc.service;

import org.json.JSONObject;

public interface IQuestionnaireApplicationService {
    JSONObject buildAnswersRequiredResponse(String message);
}
