package team.kitemc.verifymc.registration;

public class RegistrationOutcomeResolver {

    public boolean shouldAutoApprove(boolean manualReviewRequired, boolean registerAutoApprove) {
        return !manualReviewRequired && registerAutoApprove;
    }

    public String resolveStatus(RegistrationOutcome outcome) {
        return outcome == RegistrationOutcome.SUCCESS_WHITELISTED ? "approved" : "pending";
    }

    public RegistrationOutcome resolve(boolean ok,
                                       boolean manualReviewRequired,
                                       boolean questionnairePassed,
                                       boolean registerAutoApprove) {
        if (!ok) {
            return RegistrationOutcome.FAILED;
        }

        boolean autoApprove = shouldAutoApprove(manualReviewRequired, registerAutoApprove);
        if (!autoApprove && manualReviewRequired && !questionnairePassed) {
            return RegistrationOutcome.QUESTIONNAIRE_SCORING_ERROR_PENDING_REVIEW;
        }
        if (!autoApprove && questionnairePassed) {
            return RegistrationOutcome.QUESTIONNAIRE_PENDING_REVIEW;
        }
        if (autoApprove) {
            return RegistrationOutcome.SUCCESS_WHITELISTED;
        }
        return RegistrationOutcome.SUCCESS_PENDING;
    }
}
