package com.kosa.noticeserver.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
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
            System.out.println("현재 사용 중인 서비스 계정: " + clientEmail);
        } else {
            System.out.println("기본 인증 정보를 사용 중입니다 (서비스 계정 파일 아님)");
        }
        return FirebaseApp.initializeApp(options);
    }

    @Bean
    public FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
        return FirebaseMessaging.getInstance(firebaseApp);
    }
}
