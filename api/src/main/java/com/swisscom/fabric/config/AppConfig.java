package com.swisscom.fabric.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

@Configuration
@Import({EnvironmentPropertiesDumper.class})
@ComponentScan(basePackages = {"com.swisscom.fabric"})
public class AppConfig {

  private final static Logger LOGGER = LoggerFactory.getLogger(AppConfig.class);

  public class ApplicationInfo extends JsonObject {

    public ApplicationInfo(String applicationVersion) {
      super();
      this.applicationVersion = applicationVersion;
    }

    public String getApplicationVersion() {
      return applicationVersion;
    }

    private final String applicationVersion;
  }

  @Autowired
  private Environment environment;

  private static String appId = null;
  private static String appVersion = null;

  private final ApplicationInfo applicationInfo = new ApplicationInfo(getApplicationVersion());

  public Properties getAppProperties() {
    ConfigurableEnvironment applicationEnv = (ConfigurableEnvironment) environment;
    Properties properties = new Properties();
    for (PropertySource<?> entry : applicationEnv.getPropertySources()) {
      if (entry instanceof EnumerablePropertySource) {
        EnumerablePropertySource<?> enumerable = (EnumerablePropertySource<?>) entry;
        Map<String, Object> map = new LinkedHashMap<>();
        for (String name : enumerable.getPropertyNames()) {
          map.put(name, enumerable.getProperty(name));
        }
        properties.putAll(map);
      }
    }
    return properties;
  }

  public ApplicationInfo getApplicationInfo() {
    return applicationInfo;
  }

  public String getApplicationVersion() {
    return appVersion;
  }

  public static String getAppId() {
    return appId;
  }
}
