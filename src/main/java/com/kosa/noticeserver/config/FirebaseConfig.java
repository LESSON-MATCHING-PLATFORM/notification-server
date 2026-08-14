package com.kosa.noticeserver.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.List;

@Configuration
@EnableConfigurationProperties(FirebaseProperties.class)
@RequiredArgsConstructor
@Slf4j
public class FirebaseConfig {

    private final FirebaseProperties firebaseProperties;

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        List<FirebaseApp> firebaseApps = FirebaseApp.getApps();
        if (!firebaseApps.isEmpty()) {
            return firebaseApps.getFirst();
        }

        GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();
        FirebaseOptions.Builder optionsBuilder = FirebaseOptions.builder()
                .setCredentials(credentials);
        if (StringUtils.hasText(firebaseProperties.getProjectId())) {
            optionsBuilder.setProjectId(firebaseProperties.getProjectId());
        }

        if (credentials instanceof ServiceAccountCredentials) {
            String clientEmail = ((ServiceAccountCredentials) credentials).getClientEmail();
            log.info("Firebase service account credentials detected. clientEmail={}", clientEmail);
        } else {
            log.info("Firebase application default credentials detected.");
        }
        return FirebaseApp.initializeApp(optionsBuilder.build());
    }

    @Bean
    public FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
        return FirebaseMessaging.getInstance(firebaseApp);
    }
}
