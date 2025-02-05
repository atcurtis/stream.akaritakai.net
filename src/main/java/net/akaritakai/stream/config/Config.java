package net.akaritakai.stream.config;

import java.io.InputStream;
import java.net.URL;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import net.akaritakai.stream.scheduling.SchedulerAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Config {
  private static final Logger LOG = LoggerFactory.getLogger(Config.class);
  public static final SchedulerAttribute<ConfigData> KEY = SchedulerAttribute.instanceOf("configData", ConfigData.class);
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static Config INSTANCE;
  private final ConfigData _config;

  private Config(URL resource) {
    try {
      if (resource == null) {
        resource = Resources.getResource("config.json");
      }
      try (InputStream is = resource.openStream()) {
        _config = OBJECT_MAPPER.readValue(is, ConfigData.class);
      }
    } catch (Exception e) {
      LOG.error("Failed to load config", e);
      throw new RuntimeException(e);
    }
  }

  public static ConfigData getConfig() {
    return getConfig(null);
  }

  public static synchronized ConfigData getConfig(URL url) {
    if (INSTANCE == null) {
      INSTANCE = new Config(url);
    }
    return INSTANCE._config;
  }
}
