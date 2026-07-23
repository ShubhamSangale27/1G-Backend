package com.realestate.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class FirebaseInitializer {

    @Value("${app.firebase.credentials-path:}")
    private String credentialsPath;

    @Value("${app.firebase.credentials-json:}")
    private String credentialsJson;

    @Value("${app.firebase.credentials-base64:}")
    private String credentialsBase64;

    @Value("${app.firebase.enabled:false}")
    private boolean enabled;

    @PostConstruct
    public void init() {
        if (!enabled) {
            log.warn("Firebase push is disabled (app.firebase.enabled=false). Admin push sends will be recorded but not delivered.");
            return;
        }
        if (!FirebaseCredentialsLoader.hasAnySource(credentialsJson, credentialsBase64, credentialsPath)) {
            log.warn(
                    "Firebase credentials not set. Push delivery disabled until FIREBASE_CREDENTIALS_JSON, "
                            + "FIREBASE_CREDENTIALS_BASE64, or FIREBASE_CREDENTIALS_PATH is configured.");
            return;
        }
        try {
            GoogleCredentials credentials = FirebaseCredentialsLoader.load(
                    credentialsJson, credentialsBase64, credentialsPath);
            if (credentials == null) {
                log.warn("Firebase credentials could not be loaded. Push delivery disabled.");
                return;
            }
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .build();
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }
            log.info("Firebase Admin SDK initialized for push notifications");
        } catch (IOException e) {
            log.error("Failed to initialize Firebase Admin SDK: {}", e.getMessage());
        }
    }

    public boolean isReady() {
        return enabled && !FirebaseApp.getApps().isEmpty();
    }
}
