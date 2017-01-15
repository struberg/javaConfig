/*
 * Licensed under the Apache License, Version 2.0 (the &quot;License&quot;);
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 * The author licenses this file to You under the Apache License, Version 2.0
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
package io.microprofile.config;

import java.util.ServiceLoader;
import java.util.logging.Logger;

import io.microprofile.config.spi.ConfigSource;
import io.microprofile.config.spi.Converter;

/**
 * <p>This is the central class to access a {@link Config}.</p>
 *
 * <p>A {@link Config} contains the configuration for a certain
 * situation. That might be the configuration found in a certain ClassLoader
 * or even a manually created Configuration</p>
 *
 * <p>The default usage is to use {@link #getConfig()} to automatically
 * pick up the 'Configuration' for the Thread Context ClassLoader
 * (See {@link  Thread#getContextClassLoader()}). </p>
 *
 * <p>A 'Configuration' consists of the information collected from the registered
 * {@link ConfigSource}s. These {@link ConfigSource}s
 * get sorted according to their <em>ordinal</em> defined via {@link ConfigSource#getOrdinal()}.
 * That way it is possible to overwrite configuration with lower importance from outside.</p>
 *
 * <p>It is also possible to register custom {@link ConfigSource}s to
 * flexibly extend the configuration mechanism. An example would be to pick up configuration values
 * from a database table./p>
 *
 * <p>Example usage:
 *
 * <pre>
 *     String restUrl = ConfigProvider.getConfig().getValue("myproject.some.remote.service.url");
 *     Integer port = ConfigProvider.getConfig().getValue("myproject.some.remote.service.port", Integer.class);
 * </pre>
 *
 * </p>
 *
 * @author <a href="mailto:struberg@apache.org">Mark Struberg</a>
 * @author <a href="mailto:rmannibucau@apache.org">Romain Manni-Bucau</a>
 */
public class ConfigProvider {

    private static volatile SPI instance;

    /**
     * Provide a {@link Config} based on all {@link ConfigSource}s
     * of the current Thread Context ClassLoader (TCCL)
     *
     * <p>There is exactly a single Config instance per Application</p>
     */
    public static Config getConfig() {
        return loadSpi().getConfig();
    }

    /**
     * Provide a {@link Config} based on all {@link ConfigSource}s
     * of the given ClassLoader.
     *
     * <p>There is exactly a single Config instance per Application. The Application get's identified via a ClassLoader</p>
     */
    public static Config getConfig(ClassLoader forClassLoader) {
        return instance.getConfig(forClassLoader);
    }

    /**
     * Create a {@link ConfigBuilder} for providing a fresh {@link Config} instance.
     *
     * The ConfigProvider will not manage the Config instance internally.
     * That means that {@link #getConfig()} does not pick up a Config created that way.
     */
    public static ConfigBuilder newConfig() {
        return instance.newConfig();
    }

    /**
     * Create a {@link ConfigBuilder} for register a {@link Config} instance to a certain Application.
     * Invoking {@link ConfigBuilder#build()} will effectively register the Config.
     * Use {@link ConfigBuilder#forClassLoader(ClassLoader)} to define the application the Config should be for,
     * otherwise the current ThreadContextClassLoader will be used.
     *
     * A {@link Config} registered that way will get picked up on any subsequent call to {@link ConfigProvider#getConfig()}.
     *
     * @throws IllegalStateException if a {@link Config} has already been associated for the Application.
     */
    public static ConfigBuilder registerConfig() {
        return instance.registerConfig();
    }


    /**
     * A {@link Config} normally gets released if the ClassLoader it represents gets destroyed.
     * Invoke this method if you like to destroy the Config prematurely.
     */
    public static void releaseConfig(Config config) {
        instance.releaseConfig(config);
    }


    /**
     * Builder for manually creating an instance of a {@code Config}.
     *
     * @see ConfigProvider#newConfig()
     */
    public interface ConfigBuilder {
        ConfigBuilder ignoreDefaultSources();
        ConfigBuilder forClassLoader(ClassLoader loader);
        ConfigBuilder withSources(ConfigSource... sources);
        ConfigBuilder withConverters(Converter<?>... filters);
        Config build();
    }

    /**
     * This interface gets implemented internally by the Config library.
     * The implementation registers itself via {@link java.util.ServiceLoader} mechanism.
     * In an OSGi environment
     */
    public interface SPI {
        Config getConfig();
        Config getConfig(ClassLoader forClassLoader);
        ConfigBuilder newConfig();
        ConfigBuilder registerConfig();
        void releaseConfig(Config config);
    }

    /**
     * Attention, handle with care!
     * This method is not intended to be used from a user.
     * It is for integration with e.g. OSGi from within a BundleActivator.
     * TODO probably remove this and add native OSGi support later.
     */
    public static synchronized void setSPI(SPI newInstance) {
        instance = newInstance;
    }

    private static SPI loadSpi() {
        if (instance == null) {
            synchronized (SPI.class) {
                if (instance != null) {
                    return instance;
                }
                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                if (cl == null) {
                    cl = SPI.class.getClassLoader();
                }

                SPI newInstance = loadSpi(cl);

                if (newInstance == null) {
                    throw new IllegalStateException("No ConfigResolver SPI implementation found!");
                }

                instance = newInstance;
            }
        }

        return instance;
    }

    private static SPI loadSpi(ClassLoader cl) {
        if (cl == null) {
            return null;
        }

        // start from the root CL and go back down to the TCCL
        SPI instance = loadSpi(cl.getParent());

        if (instance == null) {
            ServiceLoader<SPI> sl = ServiceLoader.load(SPI.class, cl);
            for (SPI spi : sl) {
                if (instance != null) {
                    Logger.getLogger(ConfigProvider.class.getName())
                            .warning("Multiple ConfigResolver SPIs found. Ignoring " + spi.getClass().getName());
                } else {
                    instance = spi;
                }
            }
        }
        return instance;
    }


}
