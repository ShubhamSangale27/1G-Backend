package com.realestate.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
@Slf4j
public class FirebaseInitializer {

    @Value("${app.firebase.credentials-path:}")
    private String credentialsPath;

    @Value("${app.firebase.enabled:false}")
    private boolean enabled;

    @PostConstruct
    public void init() {
        if (!enabled) {
            log.warn("Firebase push is disabled (app.firebase.enabled=false). Admin push sends will be recorded but not delivered.");
            return;
        }
        if (credentialsPath == null || credentialsPath.isBlank()) {
            log.warn("Firebase credentials path not set. Push delivery disabled until FIREBASE_CREDENTIALS_PATH is configured.");
            return;
        }
        Path path = Path.of(credentialsPath.trim());
        if (!Files.isRegularFile(path)) {
            log.warn("Firebase credentials file not found at {}. Push delivery disabled.", path);
            return;
        }
        try (FileInputStream serviceAccount = new FileInputStream(path.toFile())) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
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
