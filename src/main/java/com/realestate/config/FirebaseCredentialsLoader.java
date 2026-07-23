package com.realestate.config;

import com.google.auth.oauth2.GoogleCredentials;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Loads Firebase Admin service-account credentials from env-friendly sources.
 * Priority: JSON string → Base64 → filesystem path.
 */
final class FirebaseCredentialsLoader {

    private FirebaseCredentialsLoader() {
    }

    static GoogleCredentials load(String credentialsJson, String credentialsBase64, String credentialsPath)
            throws IOException {
        if (credentialsJson != null && !credentialsJson.isBlank()) {
            try (InputStream in = new ByteArrayInputStream(credentialsJson.getBytes(StandardCharsets.UTF_8))) {
                return GoogleCredentials.fromStream(in);
            }
        }
        if (credentialsBase64 != null && !credentialsBase64.isBlank()) {
            byte[] decoded = Base64.getDecoder().decode(credentialsBase64.trim());
            try (InputStream in = new ByteArrayInputStream(decoded)) {
                return GoogleCredentials.fromStream(in);
            }
        }
        if (credentialsPath != null && !credentialsPath.isBlank()) {
            try (InputStream in = new FileInputStream(credentialsPath.trim())) {
                return GoogleCredentials.fromStream(in);
            }
        }
        return null;
    }

    static boolean hasAnySource(String credentialsJson, String credentialsBase64, String credentialsPath) {
        return (credentialsJson != null && !credentialsJson.isBlank())
                || (credentialsBase64 != null && !credentialsBase64.isBlank())
                || (credentialsPath != null && !credentialsPath.isBlank());
    }
}
