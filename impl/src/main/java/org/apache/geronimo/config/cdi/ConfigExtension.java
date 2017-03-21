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

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.enterprise.inject.spi.DeploymentException;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ProcessInjectionPoint;
import javax.inject.Provider;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * @author <a href="mailto:struberg@yahoo.de">Mark Struberg</a>
 */
public class ConfigExtension implements Extension {

    private Set<InjectionPoint> injectionPoints = new HashSet<>();

    public void collectConfigProducer(@Observes ProcessInjectionPoint<?, ?> pip) {
        ConfigProperty configProperty = pip.getInjectionPoint().getAnnotated().getAnnotation(ConfigProperty.class);
        if (configProperty != null) {
            injectionPoints.add(pip.getInjectionPoint());
        }
    }

    public void registerConfigProducer(@Observes AfterBeanDiscovery abd, BeanManager bm) {
        Set<Class> types = injectionPoints.stream()
                .filter(ip -> ip.getType() instanceof Class)
                .map(ip -> (Class) ip.getType())
                .collect(Collectors.toSet());

        // Provider and Optional are ParameterizedTypes and not a Class, so we need to add them manually
        types.add(Provider.class);
        types.add(Optional.class);

        types.forEach(type -> abd.addBean(new ConfigInjectionBean(bm, type)));
    }

    public void validate(@Observes AfterDeploymentValidation add) {
        List<String> deploymentProblems = new ArrayList<>();

        Config config = ConfigProvider.getConfig();

        for (InjectionPoint injectionPoint : injectionPoints) {
            Type type = injectionPoint.getType();
            ConfigProperty configProperty = injectionPoint.getAnnotated().getAnnotation(ConfigProperty.class);
            if (type instanceof Class) {
                // a direct injection of a ConfigProperty
                // that means a Converter must exist.
                String key = ConfigInjectionBean.getConfigKey(injectionPoint, configProperty);
                if (!config.getOptionalValue(key, (Class) type).isPresent()) {
                    deploymentProblems.add("No Config Value exists for " + key);
                }
            }
        }

        if (!deploymentProblems.isEmpty()) {
            add.addDeploymentProblem(new DeploymentException("Error while validating Configuration\n"
                                                             + String.join("\n", deploymentProblems)));
        }

    }

    public void shutdown(@Observes BeforeShutdown bsd) {
        //X TODO shutdown config
    }


}
