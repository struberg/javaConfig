package javx.config;

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

public final class ConfigFactory {
    private ConfigFactory() {
        // no-op
    }

    public static Config getConfig(final ClassLoader loader) {
        final ConfigSourceProvider[] configSourceProviders = loadArray(loader, ConfigSourceProvider.class);
        final List<ConfigSource> sources = new ArrayList<>(configSourceProviders.length);
        final Map<ConfigSource, Integer> ordinals = new HashMap<>(configSourceProviders.length);
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
        return byDefaultProvider(loader)
                .withSources(sources.toArray(new ConfigSource[sources.size()]))
                .withFilters(loadArray(loader, ConfigFilter.class))
                .build();
    }

    public static Config getConfig() {
        return getConfig(Thread.currentThread().getContextClassLoader());
    }

    public static ConfigBuilder byProvider(final Class<? extends ConfigBuilder> provider) {
        try {
            return provider.newInstance();
        } catch (final InstantiationException | IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static ConfigBuilder byDefaultProvider(final ClassLoader loader) {
         /*TODO: check size == 1 */
        return ServiceLoader.load(ConfigBuilder.class, loader).iterator().next();
    }

    private static <T> T[] loadArray(final ClassLoader loader, final Class<T> type) {
        final Collection<T> array = new ArrayList<>();
        for (final T t : ServiceLoader.load(type, loader)) { // add @Priority support?
            array.add(t);
        }
        return (T[]) array.toArray();
    }
}
