/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.geronimo.config;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.WeakHashMap;

import javx.config.Config;
import javx.config.ConfigProvider;
import javx.config.spi.ConfigFilter;
import javx.config.spi.ConfigSource;
import javx.config.spi.ConfigSourceProvider;
import javx.config.spi.Converter;
import javx.config.spi.PropertyFileConfig;

import org.apache.geronimo.config.configsource.PropertyFileConfigSourceProvider;
import org.apache.geronimo.config.configsource.SystemEnvConfigSource;
import org.apache.geronimo.config.configsource.SystemPropertyConfigSource;


/**
 * @author <a href="mailto:struberg@apache.org">Mark Struberg</a>
 */
public class DefaultConfigProvider implements ConfigProvider.SPI {

    protected static Map<ClassLoader, WeakReference<Config>> configs
            = Collections.synchronizedMap(new WeakHashMap<ClassLoader, WeakReference<Config>>());


    @Override
    public Config getConfig() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = DefaultConfigProvider.class.getClassLoader();
        }
        return getConfig(cl);
    }

    @Override
    public Config getConfig(ClassLoader forClassLoader) {

        Config config = existingConfig(forClassLoader);
        if (config == null) {
            synchronized (DefaultConfigProvider.class) {
                config = existingConfig(forClassLoader);
                if (config == null) {
                    config = createConfig(forClassLoader);
                    registerConfig(config, forClassLoader);
                }
            }
        }
        return config;
    }

    private Config existingConfig(ClassLoader forClassLoader) {
        WeakReference<Config> configRef = configs.get(forClassLoader);
        return configRef != null ? configRef.get() : null;
    }

    protected Config createConfig(ClassLoader forClassLoader) {
        ConfigImpl config = new ConfigImpl();

        List<ConfigSource> configSources = new ArrayList<>();

        configSources.addAll(getBuiltInConfigSources(forClassLoader));


        // load all ConfigSource services
        ServiceLoader<ConfigSource> configSourceLoader = ServiceLoader.load(ConfigSource.class, forClassLoader);
        for (ConfigSource configSource : configSourceLoader) {
            configSources.add(configSource);
        }

        // load all ConfigSources from ConfigSourceProviders
        ServiceLoader<ConfigSourceProvider> configSourceProviderLoader = ServiceLoader.load(ConfigSourceProvider.class, forClassLoader);
        for (ConfigSourceProvider configSourceProvider : configSourceProviderLoader) {
            configSources.addAll(configSourceProvider.getConfigSources(forClassLoader));
        }

        config.addConfigSources(configSources);
        addConverters(config, forClassLoader);

        // also register all ConfigFilters
        ServiceLoader<ConfigFilter> configFilterLoader = ServiceLoader.load(ConfigFilter.class, forClassLoader);
        for (ConfigFilter configFilter : configFilterLoader) {
            config.addConfigFilter(configFilter);
        }

        return config;
    }

    protected Collection<? extends ConfigSource> getBuiltInConfigSources(ClassLoader forClassLoader) {
        List<ConfigSource> configSources = new ArrayList<>();

        configSources.add(new SystemEnvConfigSource());
        configSources.add(new SystemPropertyConfigSource());
        configSources.addAll(new PropertyFileConfigSourceProvider("META-INF/java-config.properties", true, forClassLoader).getConfigSources(forClassLoader));
        configSources.addAll(getCustomPropertyFiles(forClassLoader));

        return configSources;
    }

    private Collection<? extends ConfigSource> getCustomPropertyFiles(ClassLoader forClassLoader) {
        List<ConfigSource> configSources = new ArrayList<>();
        ServiceLoader<PropertyFileConfig> propertyFileConfigLoader = ServiceLoader.load(PropertyFileConfig.class);
        for (PropertyFileConfig propConfig : propertyFileConfigLoader) {
            configSources.addAll(new PropertyFileConfigSourceProvider(propConfig.getPropertyFileName(), propConfig.isOptional(), forClassLoader).getConfigSources(forClassLoader));
        }

        return configSources;
    }

    protected void addConverters(Config config, ClassLoader forClassLoader) {
        ServiceLoader<Converter> converters = ServiceLoader.load(Converter.class, forClassLoader);
        for (Converter converter : converters) {
            config.addConverter(converter);
        }
    }

    private void registerConfig(Config config, ClassLoader forClassLoader) {
        synchronized (DefaultConfigProvider.class) {
            configs.put(forClassLoader, new WeakReference<>(config));
        }
    }

    @Override
    public Config newConfig() {
        return new ConfigImpl();
    }

    @Override
    public void releaseConfig(Config config) {
        Iterator<Map.Entry<ClassLoader, WeakReference<Config>>> it = configs.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<ClassLoader, WeakReference<Config>> next = it.next();
        }
    }
}
