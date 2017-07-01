package com.swisscom.fabric.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.*;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
public class EnvironmentPropertiesDumper implements EnvironmentAware {

    private final static Logger logger = LoggerFactory.getLogger(EnvironmentPropertiesDumper.class);

    @Override
    public void setEnvironment(Environment environment) {
        ConfigurableEnvironment applicationEnv = (ConfigurableEnvironment) environment;

        MutablePropertySources mutablePropertySources = applicationEnv.getPropertySources();

        if (applicationEnv.getProperty("spring.cloud.config.uri") != null) {
            logger.info("Git Configuration source found. Config server: " + applicationEnv.getProperty("spring.cloud.config.uri"));
        }

        if ((applicationEnv.getProperty("spring.cloud.consul.config.enabled") != null) && applicationEnv.getProperty("spring.cloud.consul.config.enabled").equalsIgnoreCase("true")) {
            logger.info("Consul Configuration source found. Config server: " + applicationEnv.getProperty("spring.cloud.consul.host") + ":" + applicationEnv.getProperty("spring.cloud.consul.port"));
        }

        for (PropertySource<?> source : mutablePropertySources) {
            if (source instanceof CompositePropertySource) {
                CompositePropertySource cps = (CompositePropertySource) source;
                for (PropertySource<?> ps : cps.getPropertySources()) {
                    // Spring Cloud config source
                    if (ps.getName().equalsIgnoreCase("configservice")) {
                        CompositePropertySource mps = (CompositePropertySource) ps;
                        for (PropertySource<?> rps : mps.getPropertySources()) {
                            logger.info("Git Configuration source: " + rps.getName() + "(YAML profile: " + applicationEnv.getProperty("spring.cloud.config.env") + ", Branch: " + applicationEnv.getProperty("spring.cloud.config.label") + ")");
                        }
                    }

                    // Spring Consul config source
                    if (ps.getName().equalsIgnoreCase("consul")) {
                        CompositePropertySource mps = (CompositePropertySource) ps;
                        for (PropertySource<?> rps : mps.getPropertySources()) {
                            logger.info("Consul Configuration source: " + rps.getName());
                        }
                    }
                }
            }
        }

        // dump out the properties we have
        if ((applicationEnv.getProperty("dumpPropertiesOnStartup") != null) && (applicationEnv.getProperty("dumpPropertiesOnStartup").equalsIgnoreCase("true"))) {
            logger.info("-------------------------------------");
            logger.info("profiles: " + Arrays.toString(applicationEnv.getActiveProfiles()));
            logger.info("-------------------------------------");
            for (PropertySource<?> entry : applicationEnv.getPropertySources()) {
                String sourceName = entry.getName();
                if (entry instanceof EnumerablePropertySource) {
                    EnumerablePropertySource<?> enumerable = (EnumerablePropertySource<?>) entry;
                    Map<String, Object> map = new LinkedHashMap<>();
                    for (String name : enumerable.getPropertyNames()) {
                        map.put(sourceName, enumerable.getProperty(name));
                        logger.info(sourceName + " : " + name + " - " + enumerable.getProperty(name));
                    }
                }
            }
            logger.info("-------------------------------------");
        }
    }
}
