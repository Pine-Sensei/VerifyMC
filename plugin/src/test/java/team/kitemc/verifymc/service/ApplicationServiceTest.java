package team.kitemc.verifymc.service;

import static org.junit.jupiter.api.Assertions.*;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

class ApplicationServiceTest {

    @Test
    void registrationSuccessPathShouldReturnSuccessMessage() {
        RegistrationApplicationService service = new RegistrationApplicationService();
        RegistrationApplicationService.RegistrationDecision decision = service.resolveDecision(true, false, true, true);
        JSONObject response = service.buildRegistrationResponse(decision, true, key -> key);

        assertTrue(response.getBoolean("success"));
        assertEquals("register.success_whitelisted", response.getString("msg"));
        assertEquals("register.success_whitelisted", response.getString("message"));
    }

    @Test
    void questionnaireFailurePathShouldReturnFailure() {
        QuestionnaireApplicationService service = new QuestionnaireApplicationService();
        JSONObject response = service.buildAnswersRequiredResponse("questionnaire.answers_required");

        assertFalse(response.getBoolean("success"));
        assertEquals("questionnaire.answers_required", response.getString("msg"));
        assertEquals("questionnaire.answers_required", response.getString("message"));
    }

    @Test
    void autoApproveWithManualReviewFlagAndFailedQuestionnaireShouldGoPendingReview() {
        RegistrationApplicationService service = new RegistrationApplicationService();
        RegistrationApplicationService.RegistrationDecision decision = service.resolveDecision(true, true, false, true);
        JSONObject response = service.buildRegistrationResponse(decision, true, key -> key);

        assertTrue(response.getBoolean("success"));
        assertEquals("register.questionnaire_scoring_error_pending_review", response.getString("msg"));
        assertEquals("register.questionnaire_scoring_error_pending_review", response.getString("message"));
    }

    @Test
    void autoApproveWithPassedQuestionnaireShouldStillWhitelist() {
        RegistrationApplicationService service = new RegistrationApplicationService();
        RegistrationApplicationService.RegistrationDecision decision = service.resolveDecision(true, false, true, true);

        assertTrue(decision.autoApprove());
    }

    @Test
    void reviewApprovePathShouldReturnApprovedMessage() {
        ReviewApplicationService service = new ReviewApplicationService();
        JSONObject response = service.buildReviewResponse(true, true, key -> key);

        assertTrue(response.getBoolean("success"));
        assertEquals("review.approve_success", response.getString("msg"));
    }

    @Test
    void reviewRejectPathShouldReturnRejectedMessage() {
        ReviewApplicationService service = new ReviewApplicationService();
        JSONObject response = service.buildReviewResponse(true, false, key -> key);

        assertTrue(response.getBoolean("success"));
        assertEquals("review.reject_success", response.getString("msg"));
    }
}
