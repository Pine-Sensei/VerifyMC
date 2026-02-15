package team.kitemc.verifymc.core;

import org.bukkit.plugin.Plugin;
import team.kitemc.verifymc.service.*;
import team.kitemc.verifymc.db.UserDao;
import team.kitemc.verifymc.db.AuditDao;
import team.kitemc.verifymc.service.ServiceContainer;
import team.kitemc.verifymc.mail.IMailService;
import java.util.logging.Logger;

public class ServiceInitializer {
    private static final Logger LOGGER = Logger.getLogger(ServiceInitializer.class.getName());
    
    private final ServiceContainer serviceContainer;
    
    public ServiceInitializer(Plugin plugin) {
        this.serviceContainer = new ServiceContainer(plugin);
    }
    
    public void initialize(UserDao userDao, AuditDao auditDao, 
                          IVerifyCodeService verifyCodeService,
                          IMailService mailService,
                          IAuthmeService authmeService,
                          IDiscordService discordService,
                          String webRegisterUrl) {
        serviceContainer.initialize(userDao, auditDao, verifyCodeService, 
                                   mailService, authmeService, discordService, webRegisterUrl);
        LOGGER.info("Services initialized successfully");
    }
    
    public void initializeFull(UserDao userDao, AuditDao auditDao,
                               IVerifyCodeService verifyCodeService,
                               IMailService mailService,
                               IAuthmeService authmeService,
                               IDiscordService discordService,
                               IVersionCheckService versionCheckService,
                               ICaptchaService captchaService,
                               IQuestionnaireService questionnaireService,
                               String webRegisterUrl) {
        serviceContainer.initializeFull(userDao, auditDao, verifyCodeService, 
                                        mailService, authmeService, discordService,
                                        versionCheckService, captchaService, questionnaireService, webRegisterUrl);
        LOGGER.info("All services initialized successfully");
    }
    
    public ServiceContainer getServiceContainer() {
        return serviceContainer;
    }
    
    public void shutdown() {
        serviceContainer.shutdown();
        LOGGER.info("Services shutdown");
    }
}
