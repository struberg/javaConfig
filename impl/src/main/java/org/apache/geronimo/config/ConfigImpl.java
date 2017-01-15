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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.microprofile.config.Config;
import io.microprofile.config.ConfigValue;
import io.microprofile.config.spi.ConfigSource;
import io.microprofile.config.spi.Converter;
import org.apache.geronimo.config.converters.BooleanConverter;
import org.apache.geronimo.config.converters.DoubleConverter;
import org.apache.geronimo.config.converters.FloatConverter;
import org.apache.geronimo.config.converters.IntegerConverter;
import org.apache.geronimo.config.converters.LongConverter;

import javax.annotation.Priority;

/**
 * @author <a href="mailto:struberg@apache.org">Mark Struberg</a>
 */
public class ConfigImpl implements Config {
    protected Logger logger = Logger.getLogger(ConfigImpl.class.getName());

    protected ConfigSource[] configSources = new ConfigSource[0];
    protected Map<Type, Converter> converters = new HashMap<>();


    public ConfigImpl() {
        registerDefaultConverter();
    }

    private void registerDefaultConverter() {
        converters.put(Boolean.class, BooleanConverter.INSTANCE);
        converters.put(Double.class, DoubleConverter.INSTANCE);
        converters.put(Float.class, FloatConverter.INSTANCE);
        converters.put(Integer.class, IntegerConverter.INSTANCE);
        converters.put(Long.class, LongConverter.INSTANCE);
    }

    @Override
    public String getValue(String key) {
        for (ConfigSource configSource : configSources) {
            String value = configSource.getPropertyValue(key);

            if (value != null) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "found value {0} for key {1} in ConfigSource {2}.",
                            new Object[]{value, key, configSource.getConfigName()});
                }

                return value;
            }
        }
        return null;
    }

    @Override
    public <T> T getValue(String key, Class<T> asType) {
        String value = getValue(key);
        return convert(value, asType);
    }

    @Override
    public <T> T convert(String value, Class<T> asType) {
        Converter<T> converter = getConverter(asType);
        if (value != null) {
            return converter.convert(value);
        }

        return null;
    }

    private <T> Converter getConverter(Class<T> asType) {
        Converter converter = converters.get(asType);
        if (converter == null) {
            throw new UnsupportedOperationException("No Converter registered for class " + asType);
        }
        return converter;
    }

    public ConfigValue<String> access(String key) {
        return new ConfigValueImpl<>(this, key);
    }

    @Override
    public Map<String, String> getAllProperties() {
        Map<String, String> result = new HashMap<String, String>();

        for (int i = configSources.length; i > 0; i--) {
            ConfigSource configSource = configSources[i];
            result.putAll(configSource.getProperties());
        }

        return Collections.unmodifiableMap(result);
    }


    @Override
    public ConfigSource[] getConfigSources() {
        return configSources;
    }

    public synchronized void addConfigSources(List<ConfigSource> configSourcesToAdd) {
        List<ConfigSource> allConfigSources = new ArrayList<>(Arrays.asList(configSources));
        allConfigSources.addAll(configSourcesToAdd);

        // finally put all the configSources back into the map
        configSources = sortDescending(allConfigSources);
    }


    public synchronized void addConverter(Converter<?> converter) {
        if (converter == null) {
            return;
        }

        Type targetType = getTypeOfConverter(converter.getClass());
        if (targetType == null ) {
            throw new IllegalStateException("Converter " + converter.getClass() + " must be a ParameterisedType");
        }

        Converter oldConverter = converters.get(targetType);
        if (oldConverter == null || getPriority(converter) > getPriority(oldConverter)) {
            converters.put(targetType, converter);
        }
    }

    private int getPriority(Converter<?> converter) {
        int priority = 100;
        Priority priorityAnnotation = converter.getClass().getAnnotation(Priority.class);
        if (priorityAnnotation != null) {
            priority = priorityAnnotation.value();
        }
        return priority;
    }


    public Map<Type, Converter> getConverters() {
        return converters;
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

    private Type getTypeOfConverter(Class clazz) {
        if (clazz.equals(Object.class)) {
            return null;
        }

        Type[] genericInterfaces = clazz.getGenericInterfaces();
        for (Type genericInterface : genericInterfaces) {
            if (genericInterface instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) genericInterface;
                if (pt.getRawType().equals(Converter.class)) {
                    Type[] typeArguments = pt.getActualTypeArguments();
                    if (typeArguments.length != 1) {
                        throw new IllegalStateException("Converter " + clazz + " must be a ParameterisedType");
                    }
                    return typeArguments[0];
                }
            }
        }

        return getTypeOfConverter(clazz.getSuperclass());
    }
}