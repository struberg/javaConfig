package org.apache.geronimo.config.tck.configsources;

import javx.config.spi.ConfigSource;
import javx.config.spi.ConfigSourceProvider;

import java.util.Collections;

public class DbProvider implements ConfigSourceProvider {
    @Override
    public Iterable<SourceConfiguration> getConfigSources(ClassLoader forClassLoader) {
        return Collections.<SourceConfiguration>singleton(new SourceConfiguration() {
            @Override
            public ConfigSource source() {
                return new CustomDbConfigSource();
            }

            @Override
            public int ordinal() {
                return 0;
            }
        });
    }
}
