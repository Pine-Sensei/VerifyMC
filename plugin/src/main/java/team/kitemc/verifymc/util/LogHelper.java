package team.kitemc.verifymc.util;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for consistent logging across the VerifyMC plugin.
 * Provides methods for info, warning, error, and debug logging with a unified prefix format.
 */
public class LogHelper {
    private final Logger logger;
    private final String prefix;
    private final boolean debug;
    
    /**
     * Creates a new LogHelper instance.
     * @param clazz The class for which this logger is created
     * @param prefix The prefix to use for all log messages
     * @param debug Whether debug logging is enabled
     */
    public LogHelper(Class<?> clazz, String prefix, boolean debug) {
        this.logger = Logger.getLogger(clazz.getName());
        this.prefix = prefix;
        this.debug = debug;
    }
    
    /**
     * Logs an informational message.
     * @param message The message to log
     */
    public void info(String message) {
        logger.info("[" + prefix + "] " + message);
    }
    
    /**
     * Logs a warning message.
     * @param message The message to log
     */
    public void warning(String message) {
        logger.warning("[" + prefix + "] " + message);
    }
    
    /**
     * Logs an error message.
     * @param message The message to log
     */
    public void error(String message) {
        logger.severe("[" + prefix + "] " + message);
    }
    
    /**
     * Logs an error message with an associated exception.
     * @param message The message to log
     * @param t The exception to log
     */
    public void error(String message, Throwable t) {
        logger.log(Level.SEVERE, "[" + prefix + "] " + message, t);
    }
    
    /**
     * Logs a debug message if debug mode is enabled.
     * @param message The message to log
     */
    public void debug(String message) {
        if (debug) {
            logger.info("[" + prefix + "] [DEBUG] " + message);
        }
    }
    
    /**
     * Logs a formatted debug message if debug mode is enabled.
     * @param format The message format string
     * @param args The arguments for the format string
     */
    public void debug(String format, Object... args) {
        if (debug) {
            logger.info("[" + prefix + "] [DEBUG] " + String.format(format, args));
        }
    }
}
