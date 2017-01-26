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
package io.microprofile.config.internal;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;

import io.microprofile.config.Config;

/**
 * This is the central part of the purely CDI based Configuration approach.
 *
 * It's quite messy as it has concrete classes in the API.
 * Would need to think about finding a way to hide it.
 *
 * The idea has been born in a discussion between Romain and me.
 *
 * @author <a href="mailto:struberg@apache.org">Mark Struberg</a>
 * @author <a href="mailto:rmannibucau@apache.org">Romain Manni-Bucau</a>
 */
public class ConfigExtension implements Extension {

    private Config config;

    public Config getConfig() {
        return config;
    }

    // trigger the creation of the Config subsystem early on
    public void initConfig(@Observes BeforeBeanDiscovery bbd, BeanManager beanManager) {
        ConfigBuilder configBuilder = newConfigBuilder();

        // give other Extensions a way to tweak the config, add ConfigSources etc
        // Attention those other observers must also be in Extensions!
        // Usage:
        // public void addMyConfigSource(@Observes ConfigBuilder configBuilder) { configBuilder.withConfigSource(...); }
        beanManager.fireEvent(configBuilder);

        // after that we got our ConfigBuilder filled up with all the info we need
        // we now create the Config with it
        this.config = configBuilder.build();

        // and we now send another event to the other Extensions to allow using that Config even during boot
        beanManager.fireEvent(config);
    }

    private ConfigBuilder newConfigBuilder() {
        return null; //X TODO, needs a concrete class in the API -> ugly as hell
    }


}
