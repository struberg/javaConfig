/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.geronimo.config.configsource;

import javx.config.spi.ConfigSource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class JavaConfigConfigSource implements ConfigSource {
    private final Map<String, String> values = new HashMap<>();

    public JavaConfigConfigSource() {
        try {
            final Enumeration<URL> urls = Thread.currentThread().getContextClassLoader().getResources("META-INF/java-config.properties");
            while (urls.hasMoreElements()) {
                final Properties properties = new Properties();
                try (final InputStream is = urls.nextElement().openStream()) {
                    properties.load(is);
                }
                values.putAll(Map.class.cast(properties));
            }
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public String getPropertyValue(String key) {
        return values.get(key);
    }

    @Override
    public String getConfigName() {
        return ConfigSource.class.getPackage().getName() + ".java-config";
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && JavaConfigConfigSource.class == obj.getClass();
    }
}
