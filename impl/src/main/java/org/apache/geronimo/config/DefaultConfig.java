package org.apache.geronimo.config;

import javx.config.Config;
import javx.config.spi.ConfigFilter;
import javx.config.spi.ConfigSource;
import org.apache.geronimo.config.configsource.SystemEnvConfigSource;
import org.apache.geronimo.config.configsource.SystemPropertyConfigSource;

import java.util.ArrayList;
import java.util.List;

public class DefaultConfig implements Config {
    private final ClassLoader loader;
    private final List<ConfigSource> sources;
    private final List<ConfigFilter> filters;

    public DefaultConfig(final ClassLoader loader, final List<ConfigSource> sources, final List<ConfigFilter> filters) {
        this.loader = loader;
        this.sources = new ArrayList<>(sources);
        this.filters = new ArrayList<>(filters);

        // add default sources
        this.sources.add(0, new SystemEnvConfigSource());
        this.sources.add(0, new SystemPropertyConfigSource());
    }

    @Override
    public String getValue(final String key) {
        String value = null;
        for (final ConfigSource source : sources) {
            value = source.getPropertyValue(key);
            if (value != null) {
                for (final ConfigFilter filter : filters) {
                    final String decrypted = filter.filterValue(key, value);
                    if (decrypted != null) {
                        value = decrypted;
                    }
                    // continue the loop since filters can be chained
                }
                break; // we found the value, skip other sources
            }
        }
        return value;
    }

    @Override
    public void close() {
        for (final ConfigSource source : sources) {
            if (AutoCloseable.class.isInstance(source)) {
                try {
                    AutoCloseable.class.cast(source).close();
                } catch (Exception e) {
                    // TODO :log
                }
            }
        }
        for (final ConfigFilter filter : filters) {
            if (AutoCloseable.class.isInstance(filter)) {
                try {
                    AutoCloseable.class.cast(filter).close();
                } catch (Exception e) {
                    // TODO :log
                }
            }
        }
    }
}
