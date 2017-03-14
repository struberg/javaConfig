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

package org.apache.geronimo.config.cdi;

import java.lang.annotation.Annotation;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 *
 * @author Ondrej Mihalyi
 */
@ApplicationScoped
public class ConfigPropertyInjectionProducer {
    
    private Config config;
    
    @PostConstruct
    public void init() {
        config = ConfigProvider.getConfig();
    }
    
    @Produces
    @Dependent
    @ConfigProperty
    public Float produceFloat(InjectionPoint ip) {
        return config.getValue(getConfigKeyFrom(ip), Float.class).orElse(null);
    }

    @Produces
    @Dependent
    @ConfigProperty
    public String produceString(InjectionPoint ip) {
        return config.getString(getConfigKeyFrom(ip)).orElse(null);
    }

    @Produces
    @Dependent
    @ConfigProperty
    public Boolean produceBoolean(InjectionPoint ip) {
        return config.getValue(getConfigKeyFrom(ip), Boolean.class).orElse(null);
    }

    @Produces
    @Dependent
    @ConfigProperty
    public Integer produceInteger(InjectionPoint ip) {
        return config.getValue(getConfigKeyFrom(ip), Integer.class).orElse(null);
    }

    @Produces
    @Dependent
    @ConfigProperty
    public Long produceLong(InjectionPoint ip) {
        return config.getValue(getConfigKeyFrom(ip), Long.class).orElse(null);
    }

    @Produces
    @Dependent
    @ConfigProperty
    public Double produceDouble(InjectionPoint ip) {
        return config.getValue(getConfigKeyFrom(ip), Double.class).orElse(null);
    }

    private String getConfigKeyFrom(InjectionPoint ip) {
        String configKey = getConfigKeyFromQualifier(ip);
        if (configKey == null) {
            configKey = getGeneratedConfigKey(ip);
        }
        if (configKey == null) {
            throw new IllegalArgumentException("The injection point (" + ip + ") doesn't define a config key");
        }
        return configKey;      
    }

    private String getGeneratedConfigKey(InjectionPoint ip) {
        if (ip.getBean() != null && ip.getBean().getBeanClass() != null) {
            if (ip.getMember() != null) {
                return ip.getBean().getBeanClass().getSimpleName() + "." + ip.getMember().getName();
            }
        }
        return null;
    }

    private String getConfigKeyFromQualifier(InjectionPoint ip) {
        for (Annotation q : ip.getQualifiers()) {
            if (q.annotationType().equals(ConfigProperty.class)) {
                ConfigProperty cp = (ConfigProperty)q;
                if (!cp.value().isEmpty()) {
                    return cp.value();
                }
            }
        }
        return null;
    }
}
