package org.apache.geronimo.config;

import javx.config.Config;
import javx.config.spi.ConfigSource;

public abstract class DelegateConfig implements Config {
    private final Config config;

    protected DelegateConfig(final Config config) {
        this.config = config;
    }

    protected void onValue(final String key, final String value) {
        // no-op
    }

    @Override
    public String getValue(final String key) {
        final String value = config.getValue(key);
        onValue(key, value);
        return value;
    }

    @Override
    public ConfigSource[] getSources() {
        return config.getSources();
    }

    @Override
    public void close() {
        config.close();
    }
}
