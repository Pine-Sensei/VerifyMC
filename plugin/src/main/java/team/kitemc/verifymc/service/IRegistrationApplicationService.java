package team.kitemc.verifymc.service;

import java.util.function.Function;
import org.json.JSONObject;
import team.kitemc.verifymc.service.RegistrationApplicationService.RegistrationDecision;

public interface IRegistrationApplicationService {
    RegistrationDecision resolveDecision(
            boolean registerOk,
            boolean manualReviewRequired,
            boolean questionnairePassed,
            boolean registerAutoApprove
    );
    
    boolean shouldAutoApprove(boolean manualReviewRequired, boolean registerAutoApprove);
    
    String resolveStatus(RegistrationDecision decision);
    
    JSONObject buildRegistrationResponse(RegistrationDecision decision, boolean registerOk, Function<String, String> messageResolver);
    
    JSONObject buildRegistrationResponse(
            boolean registerOk,
            boolean manualReviewRequired,
            boolean questionnairePassed,
            boolean registerAutoApprove,
            Function<String, String> messageResolver
    );
}
