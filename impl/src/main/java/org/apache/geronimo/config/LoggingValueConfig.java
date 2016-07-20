package org.apache.geronimo.config;

import javx.config.Config;

import java.util.logging.Level;
import java.util.logging.Logger;

public class LoggingValueConfig extends DelegateConfig {
    private final Logger logger;
    private final Level level;

    public LoggingValueConfig(final Logger logger, final Level level, final Config config) {
        super(config);
        this.logger = logger;
        this.level = level;
    }

    @Override
    protected void onValue(String key, String value) {
        if (!key.contains("password") && !key.contains("pwd") && !key.contains("secret")) {
            logger.log(level, "Configuration retrieved: " + key + " = " + value);
        }
    }
}
