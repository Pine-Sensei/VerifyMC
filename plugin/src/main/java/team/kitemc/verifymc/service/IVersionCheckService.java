package team.kitemc.verifymc.service;

import org.json.JSONObject;
import team.kitemc.verifymc.service.VersionCheckService.UpdateCheckResult;

import java.util.concurrent.CompletableFuture;

public interface IVersionCheckService {
    CompletableFuture<UpdateCheckResult> checkForUpdatesAsync();
    String getCurrentVersion();
    String getLatestVersion();
    boolean isUpdateAvailable();
    String getReleasesUrl();
    JSONObject getVersionInfoJson();
}
