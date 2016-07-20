package org.apache.geronimo.config;

import javx.config.Config;
import javx.config.ConfigBuilder;
import javx.config.spi.ConfigFilter;
import javx.config.spi.ConfigSource;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;

public class DefaultConfigBuilder implements ConfigBuilder {
    private final ClassLoader loader;
    private final List<ConfigSource> sources = new ArrayList<>();
    private final List<ConfigFilter> filters = new ArrayList<>();

    DefaultConfigBuilder(final ClassLoader forClassLoader) {
        this.loader = forClassLoader;
    }

    @Override
    public ConfigBuilder withSources(final ConfigSource... sources) {
        this.sources.addAll(asList(sources));
        return this;
    }

    @Override
    public ConfigBuilder withFilters(final ConfigFilter... filters) {
        this.filters.addAll(asList(filters));
        return this;
    }

    @Override
    public Config build() {
        return new DefaultConfig(loader, sources, filters);
    }
}
