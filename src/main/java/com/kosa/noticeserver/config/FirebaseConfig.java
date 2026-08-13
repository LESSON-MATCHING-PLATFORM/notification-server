package com.kosa.noticeserver.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
@Slf4j
public class FirebaseConfig {

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        GoogleCredentials applicationDefault = GoogleCredentials.getApplicationDefault();
        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(applicationDefault)
                .setProjectId("fcm-test-6dc22")
                .build();

        if (applicationDefault instanceof ServiceAccountCredentials) {
            String clientEmail = ((ServiceAccountCredentials) applicationDefault).getClientEmail();
            log.info("Firebase service account credentials detected. clientEmail={}", clientEmail);
        } else {
            log.info("Firebase application default credentials detected.");
        }
        return FirebaseApp.initializeApp(options);
    }

    @Bean
    public FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
        return FirebaseMessaging.getInstance(firebaseApp);
    }
}
