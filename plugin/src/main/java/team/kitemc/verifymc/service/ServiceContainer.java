package team.kitemc.verifymc.service;

import org.bukkit.plugin.Plugin;
import team.kitemc.verifymc.db.UserDao;
import team.kitemc.verifymc.db.AuditDao;
import team.kitemc.verifymc.mail.IMailService;
import team.kitemc.verifymc.service.impl.UserServiceImpl;
import team.kitemc.verifymc.service.impl.RegistrationServiceImpl;
import team.kitemc.verifymc.service.impl.ReviewServiceImpl;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class ServiceContainer {
    private static final Logger LOGGER = Logger.getLogger(ServiceContainer.class.getName());
    
    private final Map<Class<?>, Object> services = new HashMap<>();
    private final Plugin plugin;
    
    public ServiceContainer(Plugin plugin) {
        this.plugin = plugin;
    }
    
    public <T> void register(Class<T> type, T instance) {
        services.put(type, instance);
        LOGGER.info("Registered service: " + type.getSimpleName());
    }
    
    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> type) {
        return (T) services.get(type);
    }
    
    public <T> boolean has(Class<T> type) {
        return services.containsKey(type);
    }
    
    public void initialize(UserDao userDao, AuditDao auditDao,
                          IVerifyCodeService verifyCodeService,
                          IMailService mailService,
                          IAuthmeService authmeService,
                          IDiscordService discordService,
                          String webRegisterUrl) {
        
        IUserService userService = new UserServiceImpl(userDao);
        register(IUserService.class, userService);
        
        IReviewService reviewService = new ReviewServiceImpl(userDao, auditDao, authmeService, mailService, webRegisterUrl);
        register(IReviewService.class, reviewService);
        
        register(IVerifyCodeService.class, verifyCodeService);
        register(IMailService.class, mailService);
        register(IAuthmeService.class, authmeService);
        register(IDiscordService.class, discordService);
        
        LOGGER.info("ServiceContainer initialized with " + services.size() + " services");
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
        
        initialize(userDao, auditDao, verifyCodeService, mailService, authmeService, discordService, webRegisterUrl);
        
        if (versionCheckService != null) {
            register(IVersionCheckService.class, versionCheckService);
        }
        if (captchaService != null) {
            register(ICaptchaService.class, captchaService);
        }
        if (questionnaireService != null) {
            register(IQuestionnaireService.class, questionnaireService);
        }
        
        LOGGER.info("ServiceContainer fully initialized with " + services.size() + " services");
    }
    
    public void shutdown() {
        services.clear();
        LOGGER.info("ServiceContainer shutdown");
    }
    
    public int getServiceCount() {
        return services.size();
    }
}
