package org.apache.geronimo.config;

import javx.config.ConfigValue;
import javx.config.spi.Converter;

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
        //X TODO
        return null;
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

        return config.getValue(keyOriginal);
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
