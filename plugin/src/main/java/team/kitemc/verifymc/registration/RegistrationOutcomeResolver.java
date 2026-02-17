package team.kitemc.verifymc.registration;

public class RegistrationOutcomeResolver {

    public boolean shouldAutoApprove(boolean manualReviewRequired, boolean registerAutoApprove) {
        return registerAutoApprove && !manualReviewRequired;
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

        if (manualReviewRequired) {
            if (!questionnairePassed) {
                return scoringServiceUnavailable
                        ? RegistrationOutcome.QUESTIONNAIRE_SCORING_ERROR_PENDING_REVIEW
                        : RegistrationOutcome.QUESTIONNAIRE_PENDING_REVIEW;
            }
            // Questionnaire passed but manual review is still required
            return RegistrationOutcome.QUESTIONNAIRE_PENDING_REVIEW;
        }
        if (registerAutoApprove) {
            return RegistrationOutcome.SUCCESS_WHITELISTED;
        }
        return RegistrationOutcome.SUCCESS_PENDING;
    }
}
