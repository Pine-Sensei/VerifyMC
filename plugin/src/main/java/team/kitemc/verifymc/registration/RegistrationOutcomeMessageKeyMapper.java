package team.kitemc.verifymc.registration;

public class RegistrationOutcomeMessageKeyMapper {

    public String toMessageKey(RegistrationOutcome outcome) {
        switch (outcome) {
            case FAILED:
                return "register.failed";
            case SUCCESS_PENDING:
                return "register.success";
            case SUCCESS_WHITELISTED:
                return "register.success_whitelisted";
            case QUESTIONNAIRE_PENDING_REVIEW:
                return "register.questionnaire_pending_review";
            case QUESTIONNAIRE_SCORING_ERROR_PENDING_REVIEW:
                return "register.questionnaire_scoring_error_pending_review";
            default:
                return "register.failed";
        }
    }
}
