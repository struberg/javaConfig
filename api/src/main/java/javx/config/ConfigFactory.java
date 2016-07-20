package javx.config;

import javx.config.spi.ConfigFilter;
import javx.config.spi.ConfigSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.ServiceLoader;

public final class ConfigFactory {
    private ConfigFactory() {
        // no-op
    }

    public static Config getConfig(final ClassLoader loader) {
        return byDefaultProvider(loader)
                .withSources(loadArray(loader, ConfigSource.class))
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
