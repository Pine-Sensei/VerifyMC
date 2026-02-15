package team.kitemc.verifymc.service;

import java.util.function.Function;
import org.json.JSONObject;

public interface IReviewApplicationService {
    JSONObject buildReviewResponse(boolean reviewSuccess, boolean approve, Function<String, String> messageResolver);
}
