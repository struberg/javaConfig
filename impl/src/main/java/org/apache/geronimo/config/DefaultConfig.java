package org.apache.geronimo.config;

import javx.config.Config;
import javx.config.spi.ConfigFilter;
import javx.config.spi.ConfigSource;
import org.apache.geronimo.config.configsource.JavaConfigConfigSource;
import org.apache.geronimo.config.configsource.SystemEnvConfigSource;
import org.apache.geronimo.config.configsource.SystemPropertyConfigSource;

import java.util.ArrayList;
import java.util.List;

public class DefaultConfig implements Config {
    private final ClassLoader loader;
    private final List<ConfigSource> sources;
    private final List<ConfigFilter> filters;

    public DefaultConfig(final ClassLoader loader, final List<ConfigSource> sources, final List<ConfigFilter> filters,
                         final boolean ignoreDefaultSources) {
        this.loader = loader;
        this.sources = new ArrayList<>(sources);
        this.filters = new ArrayList<>(filters);

        if (!ignoreDefaultSources) {
            // add default sources (reverse order to be sorted after)
            this.sources.add(0, new JavaConfigConfigSource());
            this.sources.add(0, new SystemEnvConfigSource());
            this.sources.add(0, new SystemPropertyConfigSource());
        }
    }

    @Override
    public String getValue(final String key) {
        final Thread thread = Thread.currentThread();
        final ClassLoader old = thread.getContextClassLoader();
        thread.setContextClassLoader(loader);
        try {
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
        } finally {
            thread.setContextClassLoader(old);
        }
    }

    @Override
    public ConfigSource[] getSources() {
        return sources.toArray(new ConfigSource[sources.size()]);
    }

    @Override
    public void close() {
        final Thread thread = Thread.currentThread();
        final ClassLoader old = thread.getContextClassLoader();
        thread.setContextClassLoader(loader);
        try {
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
        } finally {
            thread.setContextClassLoader(old);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DefaultConfig that = (DefaultConfig) o;

        if (!sources.equals(that.sources)) return false;
        return filters.equals(that.filters);

    }

    @Override
    public int hashCode() {
        int result = sources.hashCode();
        result = 31 * result + filters.hashCode();
        return result;
    }
}
