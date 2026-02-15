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
                                       boolean registerAutoApprove) {
        boolean autoApprove = shouldAutoApprove(manualReviewRequired, registerAutoApprove);
        if (!ok) {
            return RegistrationOutcome.FAILED;
        }

        if (manualReviewRequired && !questionnairePassed) {
            return RegistrationOutcome.QUESTIONNAIRE_SCORING_ERROR_PENDING_REVIEW;
        }
        if (autoApprove) {
            return RegistrationOutcome.SUCCESS_WHITELISTED;
        }
        if (questionnairePassed) {
            return RegistrationOutcome.QUESTIONNAIRE_PENDING_REVIEW;
        }
        return RegistrationOutcome.SUCCESS_PENDING;
    }
}
