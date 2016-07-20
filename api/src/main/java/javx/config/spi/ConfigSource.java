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
package javx.config.spi;

/**
 * <p>Implement this interfaces to provide a ConfigSource.
 * A ConfigSource provides properties from a specific place, like
 * JNDI configuration, a properties file, etc</p>
 *
 * <p>The custom implementation can be 'registered' using a
 * {@link ConfigSourceProvider} or via the
 * {@link java.util.ServiceLoader} mechanism. In the later case
 * it must get registered via creating a
 * <i>META-INF/services/javax.config.spi.ConfigSource</i>
 * file and adding the fully qualified class name of your ConfigSource
 * implementation into it. </p>
 *
 * @author <a href="mailto:struberg@apache.org">Mark Struberg</a>
 * @author <a href="mailto:gpetracek@apache.org">Gerhard Petracek</a>
 */
public interface ConfigSource {
    /**
     * @param key for the property
     * @return configured value or <code>null</code> if this ConfigSource doesn't provide any value for the given key.
     */
    String getPropertyValue(String key);

    /**
     * The name of the config might be used for logging or analysis of configured values.
     *
     * @return the 'name' of the configuration source, e.g. 'property-file mylocation/myproperty.properties'
     */
    String getConfigName();
}
