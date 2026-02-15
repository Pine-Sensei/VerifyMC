package team.kitemc.verifymc.registration;

public class RegistrationOutcomeResolver {

    public boolean shouldAutoApprove(boolean manualReviewRequired, boolean registerAutoApprove) {
        return registerAutoApprove;
    }

    public String resolveStatus(RegistrationOutcome outcome) {
        return outcome == RegistrationOutcome.SUCCESS_WHITELISTED ? "approved" : "pending";
    }

    public RegistrationOutcome resolve(boolean ok,
                                       boolean manualReviewRequired,
                                       boolean scoringServiceUnavailable,
                                       boolean questionnairePassed,
                                       boolean registerAutoApprove) {
        if (!ok) {
            return RegistrationOutcome.FAILED;
        }

        if (manualReviewRequired && scoringServiceUnavailable && !questionnairePassed) {
            return RegistrationOutcome.QUESTIONNAIRE_SCORING_ERROR_PENDING_REVIEW;
        }
        if (manualReviewRequired) {
            return RegistrationOutcome.QUESTIONNAIRE_PENDING_REVIEW;
        }
        if (registerAutoApprove) {
            return RegistrationOutcome.SUCCESS_WHITELISTED;
        }
        if (questionnairePassed) {
            return RegistrationOutcome.QUESTIONNAIRE_PENDING_REVIEW;
        }
        return RegistrationOutcome.SUCCESS_PENDING;
    }
}
