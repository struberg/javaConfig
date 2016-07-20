package javx.config;

import java.util.Iterator;
import java.util.ServiceLoader;

public final class ConfigFactory {
    private ConfigFactory() {
        // no-op
    }

    public static Config getConfig(final ClassLoader loader) {
        return byDefaultProvider(loader).build();
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
        final Iterator<ConfigBuilder> iterator = ServiceLoader.load(ConfigBuilder.class, loader).iterator();
        if (!iterator.hasNext()) {
            throw new IllegalStateException("No ConfigBuilder found");
        }
        final ConfigBuilder builder = iterator.next();
        if (iterator.hasNext()) {
            throw new IllegalStateException("Found multiple ConfigBuilder: " + builder + ", " + iterator.next());
        }
        return builder.withClassLoader(loader);
    }
}
