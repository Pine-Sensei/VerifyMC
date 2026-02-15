package team.kitemc.verifymc.service;

import org.json.JSONObject;
import team.kitemc.verifymc.service.QuestionnaireService.QuestionAnswer;
import team.kitemc.verifymc.service.QuestionnaireService.QuestionnaireResult;

import java.util.Map;

public interface IQuestionnaireService {
    boolean isEnabled();
    int getPassScore();
    boolean hasTextQuestions();
    JSONObject getQuestionnaire(String language);
    QuestionnaireResult evaluateAnswers(Map<Integer, QuestionAnswer> answers);
    void reload();
}
