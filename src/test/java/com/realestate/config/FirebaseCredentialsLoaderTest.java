package com.realestate.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FirebaseCredentialsLoaderTest {

    @Test
    void hasAnySource_allBlank_returnsFalse() {
        assertFalse(FirebaseCredentialsLoader.hasAnySource("", "  ", null));
    }

    @Test
    void hasAnySource_jsonSet_returnsTrue() {
        assertTrue(FirebaseCredentialsLoader.hasAnySource("{\"type\":\"service_account\"}", "", ""));
    }

    @Test
    void load_allBlank_returnsNull() throws IOException {
        assertNull(FirebaseCredentialsLoader.load("", null, "   "));
    }

    @Test
    void load_jsonProvided_usesJsonBranch() {
        assertThrows(IOException.class, () ->
                FirebaseCredentialsLoader.load("not-valid-json", null, null));
    }

    @Test
    void load_jsonAndPathProvided_prefersJson() throws IOException {
        Path temp = Files.createTempFile("firebase-creds", ".json");
        Files.writeString(temp, "{\"type\":\"service_account\"}", StandardCharsets.UTF_8);
        try {
            assertThrows(IOException.class, () ->
                    FirebaseCredentialsLoader.load("not-valid-json", null, temp.toString()));
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    @Test
    void load_base64Provided_usesBase64Branch() {
        String encoded = Base64.getEncoder().encodeToString("not-valid-json".getBytes(StandardCharsets.UTF_8));
        assertThrows(IOException.class, () ->
                FirebaseCredentialsLoader.load(null, encoded, null));
    }

    @Test
    void load_base64AndPathProvided_prefersBase64() throws IOException {
        Path temp = Files.createTempFile("firebase-creds", ".json");
        Files.writeString(temp, "{\"type\":\"service_account\"}", StandardCharsets.UTF_8);
        String encoded = Base64.getEncoder().encodeToString("not-valid-json".getBytes(StandardCharsets.UTF_8));
        try {
            assertThrows(IOException.class, () ->
                    FirebaseCredentialsLoader.load(null, encoded, temp.toString()));
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    @Test
    void load_pathProvided_usesPathBranch() throws IOException {
        Path temp = Files.createTempFile("firebase-creds", ".json");
        Files.writeString(temp, "not-valid-json", StandardCharsets.UTF_8);
        try {
            assertThrows(IOException.class, () ->
                    FirebaseCredentialsLoader.load(null, null, temp.toString()));
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    @Test
    void load_validJsonAndBase64RepresentSamePayload() throws IOException {
        String json = "{\"type\":\"service_account\",\"project_id\":\"one-guntha\"}";
        String encoded = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        assertThrows(IOException.class, () -> FirebaseCredentialsLoader.load(json, null, null));
        assertThrows(IOException.class, () -> FirebaseCredentialsLoader.load(null, encoded, null));
    }
}
