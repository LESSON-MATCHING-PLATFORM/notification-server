package com.kosa.noticeserver.config;

import com.google.cloud.secretmanager.v1.AccessSecretVersionResponse;
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import com.google.cloud.secretmanager.v1.SecretVersionName;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Properties;

@Slf4j
public class EnvConfigInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        ConfigurableEnvironment env = applicationContext.getEnvironment();

        String gcpProjectId = env.getProperty("spring.cloud.gcp.project-id", "lessonplatform-495307");

        try (SecretManagerServiceClient client = SecretManagerServiceClient.create()) {

            SecretVersionName secretVersionName = SecretVersionName.of(gcpProjectId, "app-env", "latest");

            AccessSecretVersionResponse accessSecretVersionResponse = client.accessSecretVersion(secretVersionName);
            String secret = accessSecretVersionResponse.getPayload().getData().toStringUtf8();

            Properties properties = new Properties();
            properties.load(new StringReader(secret));

            HashMap<String, Object> propertiesMap = new HashMap<>();
            properties.forEach((k, v) -> propertiesMap.put(k.toString(), v));

            MapPropertySource source = new MapPropertySource("gcp-app-env", propertiesMap);
            env.getPropertySources().addFirst(source);

            log.info("GCP App Env Config Initialized");
        } catch (Exception e) {
            log.error("GCP App Env Config Initialization Failed", e);
        }

    }
}
