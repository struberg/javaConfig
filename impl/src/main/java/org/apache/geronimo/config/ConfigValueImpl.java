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

import io.microprofile.config.ConfigValue;
import io.microprofile.config.spi.Converter;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author <a href="mailto:struberg@apache.org">Mark Struberg</a>
 */
public class ConfigValueImpl<T> implements ConfigValue<T> {
    private static final Logger logger = Logger.getLogger(ConfigValueImpl.class.getName());

    private final ConfigImpl config;

    private String keyOriginal;

    private String keyResolved;

    private Class<?> configEntryType = String.class;

    private String[] lookupChain;

    private boolean withDefault = false;
    private T defaultValue;


    private Converter<?> converter;

    private boolean evaluateVariables = false;

    private boolean logChanges = false;

    private long cacheTimeMs = -1;
    private volatile long reloadAfter = -1;
    private T lastValue = null;

    public ConfigValueImpl(ConfigImpl config, String key) {
        this.config = config;
        this.keyOriginal = key;
    }

    @Override
    public <N> ConfigValue<N> as(Class<N> clazz) {
        configEntryType = clazz;
        this.converter = null;
        return (ConfigValue<N>) this;
    }

    @Override
    public ConfigValue<T> withDefault(T value) {
        defaultValue = value;
        withDefault = true;
        return this;
    }

    @Override
    public ConfigValue<T> withStringDefault(String value) {
        if (value == null || value.isEmpty())
        {
            throw new RuntimeException("Empty String or null supplied as string-default value for property "
                    + keyOriginal);
        }

        defaultValue = convert(value);
        withDefault = true;
        return this;
    }

    @Override
    public ConfigValue<T> cacheFor(long value, TimeUnit timeUnit) {
        this.cacheTimeMs = timeUnit.toMillis(value);
        return this;
    }

    @Override
    public ConfigValue<T> evaluateVariables(boolean evaluateVariables) {
        this.evaluateVariables = evaluateVariables;
        return this;
    }

    @Override
    public ConfigValue<T> withLookupChain(String... postfixNames) {
        this.lookupChain = postfixNames;
        return this;
    }

    @Override
    public ConfigValue<T> logChanges(boolean logChanges) {
        this.logChanges = logChanges;
        return this;
    }

    @Override
    public T getValue() {
        long now = -1;
        if (cacheTimeMs > 0)
        {
            now = System.currentTimeMillis();
            if (now <= reloadAfter)
            {
                return lastValue;
            }
        }

        String valueStr = resolveStringValue();
        T value = convert(valueStr);

        if (withDefault)
        {
            value = fallbackToDefaultIfEmpty(keyResolved, value, defaultValue);
        }

        if (logChanges && (value != null && !value.equals(lastValue) || (value == null && lastValue != null)) )
        {
            logger.log(Level.INFO, "New value {0} for key {1}.",
                    new Object[]{config.filterConfigValueForLog(keyOriginal, valueStr), keyOriginal});
        }

        lastValue = value;

        if (cacheTimeMs > 0)
        {
            reloadAfter = now + cacheTimeMs;
        }

        return value;
    }

    private String resolveStringValue() {
        //X TODO implement lookupChain

        String value = config.getValue(keyOriginal);
        if (evaluateVariables)
        {
            // recursively resolve any ${varName} in the value
            int startVar = 0;
            while ((startVar = value.indexOf("${", startVar)) >= 0)
            {
                int endVar = value.indexOf("}", startVar);
                if (endVar <= 0)
                {
                    break;
                }
                String varName = value.substring(startVar + 2, endVar);
                if (varName.isEmpty())
                {
                    break;
                }
                String variableValue = config.access(varName).evaluateVariables(true).withLookupChain(lookupChain).getValue();
                if (variableValue != null)
                {
                    value = value.replace("${" + varName + "}", variableValue);
                }
                startVar++;
            }
        }
        return value;
    }

    @Override
    public String getKey() {
        return keyOriginal;
    }

    @Override
    public String getResolvedKey() {
        return keyResolved;
    }

    @Override
    public T getDefaultValue() {
        return defaultValue;
    }

    private T convert(String value) {
        if (String.class == configEntryType) {
            return (T) value;
        }

        Converter converter = config.getConverters().get(configEntryType);
        if (converter == null) {
            throw new IllegalStateException("No Converter for type " + configEntryType);
        }

        return (T) converter.convert(value);
    }

    private T fallbackToDefaultIfEmpty(String key, T value, T defaultValue) {
        if (value == null || (value instanceof String && ((String)value).isEmpty()))
        {
            logger.log(Level.FINE, "no configured value found for key {0}, using default value {1}.",
                    new Object[]{key, defaultValue});

            return defaultValue;
        }

        return value;
    }

}
