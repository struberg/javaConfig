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

/**
 * {@link ConfigSource} which uses {@link System#getProperties()}
 *
 * @author <a href="mailto:struberg@apache.org">Mark Struberg</a>
 */
public class SystemPropertyConfigSource implements ConfigSource {
    @Override
    public String getPropertyValue(String key) { // TODO: think to use a Map cause at runtime this is slow (lock)
        return System.getProperty(key);
    }

    @Override
    public String getConfigName() {
        return ConfigSource.class.getPackage().getName() + ".system-properties";
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && SystemPropertyConfigSource.class == obj.getClass();
    }
}
