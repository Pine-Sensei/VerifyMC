package team.kitemc.verifymc.service;

import java.util.function.Function;
import org.json.JSONObject;
import team.kitemc.verifymc.registration.RegistrationOutcome;
import team.kitemc.verifymc.registration.RegistrationOutcomeMessageKeyMapper;
import team.kitemc.verifymc.registration.RegistrationOutcomeResolver;
import team.kitemc.verifymc.web.ApiResponseFactory;

public class RegistrationApplicationService {
    private final RegistrationOutcomeResolver resolver = new RegistrationOutcomeResolver();
    private final RegistrationOutcomeMessageKeyMapper messageKeyMapper = new RegistrationOutcomeMessageKeyMapper();

    public RegistrationDecision resolveDecision(
            boolean registerOk,
            boolean manualReviewRequired,
            boolean questionnairePassed,
            boolean registerAutoApprove,
            boolean scoringServiceUnavailable
    ) {
        boolean autoApprove = resolver.shouldAutoApprove(manualReviewRequired, questionnairePassed, registerAutoApprove);
        RegistrationOutcome outcome = resolver.resolve(registerOk, manualReviewRequired, questionnairePassed, registerAutoApprove, scoringServiceUnavailable);
        return new RegistrationDecision(autoApprove, outcome);
    }

    public boolean shouldAutoApprove(boolean manualReviewRequired, boolean questionnairePassed, boolean registerAutoApprove) {
        return resolver.shouldAutoApprove(manualReviewRequired, questionnairePassed, registerAutoApprove);
    }

    public String resolveStatus(RegistrationDecision decision) {
        return resolver.resolveStatus(decision.outcome());
    }

    public JSONObject buildRegistrationResponse(RegistrationDecision decision, boolean registerOk, Function<String, String> messageResolver) {
        String message = messageResolver.apply(messageKeyMapper.toMessageKey(decision.outcome()));
        return ApiResponseFactory.create(registerOk, message);
    }

    public JSONObject buildRegistrationResponse(
            boolean registerOk,
            boolean manualReviewRequired,
            boolean questionnairePassed,
            boolean registerAutoApprove,
            boolean scoringServiceUnavailable,
            Function<String, String> messageResolver
    ) {
        RegistrationDecision decision = resolveDecision(registerOk, manualReviewRequired, questionnairePassed, registerAutoApprove, scoringServiceUnavailable);
        return buildRegistrationResponse(decision, registerOk, messageResolver);
    }

    public record RegistrationDecision(boolean autoApprove, RegistrationOutcome outcome) {}
}
