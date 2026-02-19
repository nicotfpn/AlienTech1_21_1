package net.nicotfpn.alientech.util;

import net.nicotfpn.alientech.AlienTech;
import org.slf4j.Logger;

/**
 * Safe debug instrumentation system.
 * <p>
 * Provides conditional logging that can be enabled/disabled.
 * All logging is lightweight and disabled by default.
 * <p>
 * Usage:
 * <pre>
 * if (AlienTechDebug.ENTROPY.isEnabled()) {
 *     AlienTechDebug.ENTROPY.log("Entropy transfer: {} -> {}", source, dest);
 * }
 * </pre>
 */
public final class AlienTechDebug {

    private static final Logger LOGGER = AlienTech.LOGGER;

    // Debug flags - set to true to enable specific debug logging
    // These should be configurable via config file in production
    private static final boolean ENABLE_ENTROPY_DEBUG = false;
    private static final boolean ENABLE_EVOLUTION_DEBUG = false;
    private static final boolean ENABLE_MACHINE_DEBUG = false;
    private static final boolean ENABLE_ABILITY_DEBUG = false;

    private AlienTechDebug() {
        // Static utility class
    }

    /**
     * Entropy system debug logger.
     */
    public static final DebugLogger ENTROPY = new DebugLogger(ENABLE_ENTROPY_DEBUG, "ENTROPY");

    /**
     * Evolution system debug logger.
     */
    public static final DebugLogger EVOLUTION = new DebugLogger(ENABLE_EVOLUTION_DEBUG, "EVOLUTION");

    /**
     * Machine system debug logger.
     */
    public static final DebugLogger MACHINE = new DebugLogger(ENABLE_MACHINE_DEBUG, "MACHINE");

    /**
     * Ability system debug logger.
     */
    public static final DebugLogger ABILITY = new DebugLogger(ENABLE_ABILITY_DEBUG, "ABILITY");

    /**
     * Lightweight debug logger that only logs when enabled.
     */
    public static final class DebugLogger {
        private final boolean enabled;
        private final String prefix;

        private DebugLogger(boolean enabled, String prefix) {
            this.enabled = enabled;
            this.prefix = "[" + prefix + "] ";
        }

        /**
         * Check if this logger is enabled.
         * 
         * @return true if logging is enabled
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * Log a debug message with format string.
         * Only logs if enabled.
         * 
         * @param message the message format
         * @param args the format arguments
         */
        public void log(String message, Object... args) {
            if (enabled) {
                LOGGER.debug(prefix + message, args);
            }
        }

        /**
         * Log a debug message.
         * Only logs if enabled.
         * 
         * @param message the message
         */
        public void log(String message) {
            if (enabled) {
                LOGGER.debug(prefix + message);
            }
        }
    }
}
