package com.realestate.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest
@ActiveProfiles("test")
class FirebaseInitializerIntegrationTest {

    @SpringBootTest
    @ActiveProfiles("test")
    @TestPropertySource(properties = "app.firebase.enabled=false")
    static class WhenDisabled {

        @Autowired
        private FirebaseInitializer firebaseInitializer;

        @Test
        void isReady_returnsFalse() {
            assertFalse(firebaseInitializer.isReady());
        }
    }

    @SpringBootTest
    @ActiveProfiles("test")
    @TestPropertySource(properties = {
            "app.firebase.enabled=true",
            "app.firebase.credentials-json=not-valid-json"
    })
    static class WhenJsonInvalid {

        @Autowired
        private FirebaseInitializer firebaseInitializer;

        @Test
        void isReady_returnsFalse() {
            assertFalse(firebaseInitializer.isReady());
        }
    }

    @SpringBootTest
    @ActiveProfiles("test")
    @TestPropertySource(properties = "app.firebase.enabled=true")
    static class WhenBase64Invalid {

        private static final String INVALID_JSON_B64 = Base64.getEncoder()
                .encodeToString("not-valid-json".getBytes(StandardCharsets.UTF_8));

        @DynamicPropertySource
        static void firebaseProperties(DynamicPropertyRegistry registry) {
            registry.add("app.firebase.credentials-base64", () -> INVALID_JSON_B64);
        }

        @Autowired
        private FirebaseInitializer firebaseInitializer;

        @Test
        void isReady_returnsFalse() {
            assertFalse(firebaseInitializer.isReady());
        }
    }

    @SpringBootTest
    @ActiveProfiles("test")
    @TestPropertySource(properties = "app.firebase.enabled=true")
    static class WhenPathInvalid {

        private static Path credentialsFile;

        @DynamicPropertySource
        static void firebaseProperties(DynamicPropertyRegistry registry) throws IOException {
            credentialsFile = Files.createTempFile("firebase-test-creds", ".json");
            Files.writeString(credentialsFile, "not-valid-json", StandardCharsets.UTF_8);
            registry.add("app.firebase.credentials-path", () -> credentialsFile.toString());
        }

        @Autowired
        private FirebaseInitializer firebaseInitializer;

        @Test
        void isReady_returnsFalse() {
            assertFalse(firebaseInitializer.isReady());
        }
    }
}
