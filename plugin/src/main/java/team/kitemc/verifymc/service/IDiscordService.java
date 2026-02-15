package team.kitemc.verifymc.service;

import team.kitemc.verifymc.service.DiscordService.DiscordCallbackResult;
import team.kitemc.verifymc.service.DiscordService.DiscordUser;

public interface IDiscordService {
    boolean isEnabled();
    boolean isRequired();
    String generateAuthUrl(String username);
    DiscordCallbackResult handleCallback(String code, String state);
    boolean isLinked(String username);
    String getLinkedDiscordId(String username);
    DiscordUser getLinkedUser(String username);
    boolean unlinkDiscord(String username);
    void loadConfig();
}
