package org.apache.geronimo.config;

import javx.config.Config;
import javx.config.ConfigBuilder;
import javx.config.spi.ConfigFilter;
import javx.config.spi.ConfigSource;
import javx.config.spi.ConfigSourceProvider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import static java.util.Arrays.asList;

public class DefaultConfigBuilder implements ConfigBuilder {
    private ClassLoader loader;
    private final List<ConfigSource> sources = new ArrayList<>();
    private final List<ConfigFilter> filters = new ArrayList<>();
    private boolean auto = true;
    private boolean ignoreDefaultSources;

    @Override
    public ConfigBuilder ignoreDefaultSources() {
        this.ignoreDefaultSources = true;
        return this;
    }

    @Override
    public ConfigBuilder withClassLoader(final ClassLoader loader) {
        this.loader = loader;
        return this;
    }

    @Override
    public ConfigBuilder withSources(final ConfigSource... sources) {
        this.auto = false;
        this.sources.addAll(asList(sources));
        return this;
    }

    @Override
    public ConfigBuilder withFilters(final ConfigFilter... filters) {
        this.auto = false;
        this.filters.addAll(asList(filters));
        return this;
    }

    @Override
    public Config build() {
        if (auto) {
            final Collection<ConfigSourceProvider> configSourceProviders = doLoad(loader, ConfigSourceProvider.class);
            final List<ConfigSource> sources = new ArrayList<>(configSourceProviders.size());
            final Map<ConfigSource, Integer> ordinals = new HashMap<>(configSourceProviders.size());
            for (final ConfigSourceProvider p : configSourceProviders) {
                final Iterable<ConfigSourceProvider.SourceConfiguration> configSources = p.getConfigSources(loader);
                for (final ConfigSourceProvider.SourceConfiguration c : configSources) {
                    final ConfigSource source = c.source();
                    sources.add(source);
                    ordinals.put(source, c.ordinal());
                }
            }
            Collections.sort(sources, new Comparator<ConfigSource>() {
                @Override
                public int compare(final ConfigSource o1, final ConfigSource o2) {
                    return ordinals.get(o1) - ordinals.get(o2);
                }
            });
            withSources(sources.toArray(new ConfigSource[sources.size()]));

            final Collection<ConfigFilter> filters = doLoad(loader, ConfigFilter.class);
            withFilters(filters.toArray(new ConfigFilter[filters.size()]));
        }
        return new DefaultConfig(loader, sources, filters, ignoreDefaultSources);
    }

    private static <T> Collection<T> doLoad(final ClassLoader loader, final Class<T> type) {
        final Collection<T> collection = new ArrayList<>();
        for (final T t : ServiceLoader.load(type, loader)) { // add @Priority support?
            collection.add(t);
        }
        return collection;
    }
}
