package team.kitemc.verifymc.registration;

public class RegistrationOutcomeResolver {

    public boolean shouldAutoApprove(boolean manualReviewRequired, boolean questionnairePassed, boolean registerAutoApprove) {
        if (!registerAutoApprove) {
            return false;
        }
        return questionnairePassed || !manualReviewRequired;
    }

    public String resolveStatus(RegistrationOutcome outcome) {
        return outcome == RegistrationOutcome.SUCCESS_WHITELISTED ? "approved" : "pending";
    }

    public RegistrationOutcome resolve(boolean ok,
                                       boolean manualReviewRequired,
                                       boolean questionnairePassed,
                                       boolean registerAutoApprove,
                                       boolean scoringServiceUnavailable) {
        if (!ok) {
            return RegistrationOutcome.FAILED;
        }

        if (registerAutoApprove) {
            if (questionnairePassed) {
                return RegistrationOutcome.SUCCESS_WHITELISTED;
            }
            if (manualReviewRequired) {
                return scoringServiceUnavailable
                        ? RegistrationOutcome.QUESTIONNAIRE_SCORING_ERROR_PENDING_REVIEW
                        : RegistrationOutcome.QUESTIONNAIRE_PENDING_REVIEW;
            }
            return RegistrationOutcome.SUCCESS_WHITELISTED;
        }

        if (manualReviewRequired) {
            return scoringServiceUnavailable
                    ? RegistrationOutcome.QUESTIONNAIRE_SCORING_ERROR_PENDING_REVIEW
                    : RegistrationOutcome.QUESTIONNAIRE_PENDING_REVIEW;
        }

        return RegistrationOutcome.SUCCESS_PENDING;
    }
}
