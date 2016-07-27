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
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import javx.config.Config;
import javx.config.ConfigProvider;


/**
 * @author <a href="mailto:struberg@apache.org">Mark Struberg</a>
 */
public class DefaultConfigProvider implements ConfigProvider.SPI {

    protected static Map<ClassLoader, WeakReference<Config>> configs = Collections.synchronizedMap(new WeakHashMap<ClassLoader, WeakReference<Config>>());


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
        return newConfig().forClassLoader(forClassLoader).build();
    }

    private void registerConfig(Config config, ClassLoader forClassLoader) {
        synchronized (DefaultConfigProvider.class) {
            configs.put(forClassLoader, new WeakReference<Config>(config));
        }
    }

    @Override
    public ConfigProvider.ConfigBuilder newConfig() {
        return new DefaultConfigBuilder();
    }

    @Override
    public void releaseConfig(Config config) {
        if (config != null) {
            synchronized (DefaultConfigProvider.class) {
                Iterator<Map.Entry<ClassLoader, WeakReference<Config>>> it = configs.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<ClassLoader, WeakReference<Config>> entry = it.next();
                    if (entry.getValue().get() != null && entry.getValue().get() == config) {
                        it.remove();
                        break;
                    }
                }
            }
        }
    }
}
