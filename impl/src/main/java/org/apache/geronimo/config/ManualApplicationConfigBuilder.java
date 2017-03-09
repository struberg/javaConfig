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

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

/**
 * @author <a href="mailto:struberg@yahoo.de">Mark Struberg</a>
 */
public class ManualApplicationConfigBuilder extends DefaultConfigBuilder {
    private DefaultConfigProvider configProvider;

    public ManualApplicationConfigBuilder(DefaultConfigProvider configProvider) {
        this.configProvider = configProvider;
    }

    @Override
    public synchronized Config build() {
        Config config = super.build();
        ClassLoader cl = forClassLoader;
        if (cl == null) {
            cl = Thread.currentThread().getContextClassLoader();
            if (cl == null) {
                cl = ConfigProviderResolver.class.getClassLoader();
            }
        }
        Config oldConfig = configProvider.existingConfig(cl);
        if (oldConfig != null) {
            throw new IllegalStateException("This Application already has a registered Configuration!");
        }
        configProvider.registerConfig(config, cl);
        return config;
    }
}
