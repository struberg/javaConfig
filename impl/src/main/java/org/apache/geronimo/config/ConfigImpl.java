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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javx.config.Config;
import javx.config.spi.ConfigFilter;
import javx.config.spi.ConfigSource;

/**
 * @author <a href="mailto:struberg@apache.org">Mark Struberg</a>
 */
public class ConfigImpl implements Config {
    protected Logger logger = Logger.getLogger(ConfigImpl.class.getName());

    protected ConfigSource[] configSources = new ConfigSource[0];
    protected List<ConfigFilter> configFilters = new ArrayList<>();

    @Override
    public String getValue(String key) {
        for (ConfigSource configSource : configSources) {
            String value = configSource.getPropertyValue(key);

            if (value != null) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "found value {0} for key {1} in ConfigSource {2}.",
                            new Object[]{filterConfigValueForLog(key, value), key, configSource.getConfigName()});
                }

                return filterConfigValue(key, value);
            }
        }
        return null;
    }

    @Override
    public Map<String, String> getAllProperties() {
        Map<String, String> result = new HashMap<String, String>();

        for (int i = configSources.length; i > 0; i--) {
            ConfigSource configSource = configSources[i];
            if (configSource.isScannable()) {
                result.putAll(configSource.getProperties());
            }
        }

        // now filter them
        for (Map.Entry<String, String> entries : result.entrySet()) {
            entries.setValue(filterConfigValue(entries.getKey(), entries.getValue()));
        }

        return Collections.unmodifiableMap(result);
    }

    @Override
    public String filterConfigValue(String key, String value) {
        String filteredValue = value;

        for (ConfigFilter filter : configFilters) {
            filteredValue = filter.filterValue(key, filteredValue);
        }
        return filteredValue;
    }

    @Override
    public String filterConfigValueForLog(String key, String value) {
        String logValue = value;

        for (ConfigFilter filter : configFilters) {
            logValue = filter.filterValueForLog(key, logValue);
        }

        return logValue;
    }

    @Override
    public ConfigSource[] getConfigSources() {
        return configSources;
    }

    @Override
    public synchronized void addConfigSources(List<ConfigSource> configSourcesToAdd) {
        List<ConfigSource> allConfigSources = new ArrayList<>(Arrays.asList(configSources));
        allConfigSources.addAll(configSourcesToAdd);

        // finally put all the configSources back into the map
        configSources = sortDescending(allConfigSources);
    }

    @Override
    public synchronized  void addConfigFilter(ConfigFilter configFilter) {
        configFilters.add(configFilter);

    }

    protected ConfigSource[] sortDescending(List<ConfigSource> configSources) {
        Collections.sort(configSources, new Comparator<ConfigSource>() {
            @Override
            public int compare(ConfigSource configSource1, ConfigSource configSource2) {
                return (configSource1.getOrdinal() > configSource2.getOrdinal()) ? -1 : 1;
            }
        });
        return configSources.toArray(new ConfigSource[configSources.size()]);

    }
}