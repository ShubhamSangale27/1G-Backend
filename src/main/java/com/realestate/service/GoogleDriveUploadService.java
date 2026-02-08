package com.realestate.service;

import com.google.api.client.http.InputStreamContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.Permission;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

/**
 * Uploads files to a Google Drive folder and returns publicly accessible view URLs.
 * Configure GOOGLE_DRIVE_FOLDER_ID and either GOOGLE_DRIVE_CREDENTIALS_PATH or GOOGLE_DRIVE_CREDENTIALS_JSON.
 * See GOOGLE_DRIVE_SETUP.md for setup.
 */
@Service
@Slf4j
public class GoogleDriveUploadService {

    private static final String PUBLIC_VIEW_URL_PREFIX = "https://drive.google.com/uc?export=view&id=";
    private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE_FILE);

    @Value("${app.google-drive.folder-id:}")
    private String folderId;

    @Value("${app.google-drive.credentials-path:}")
    private String credentialsPath;

    @Value("${app.google-drive.credentials-json:}")
    private String credentialsJson;

    private Drive drive;

    @PostConstruct
    public void init() {
        if (!isConfigured()) {
            log.debug("Google Drive upload not configured (missing folder-id or credentials); local upload will be used.");
            return;
        }
        try {
            GoogleCredentials credentials = loadCredentials();
            if (credentials == null) {
                log.warn("Google Drive credentials could not be loaded; local upload will be used.");
                return;
            }
            credentials = credentials.createScoped(SCOPES);
            drive = new Drive.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance(),
                    new HttpCredentialsAdapter(credentials))
                    .setApplicationName("realestate-backend")
                    .build();
            log.info("Google Drive upload configured for folder id: {}", folderId);
        } catch (Exception e) {
            log.warn("Failed to initialize Google Drive client; local upload will be used.", e);
        }
    }

    public boolean isConfigured() {
        return folderId != null && !folderId.isBlank()
                && ((credentialsPath != null && !credentialsPath.isBlank())
                || (credentialsJson != null && !credentialsJson.isBlank()));
    }

    public boolean isAvailable() {
        return drive != null && isConfigured();
    }

    /**
     * Upload bytes to Google Drive, set "anyone with link can view", and return the public view URL.
     */
    public String upload(byte[] bytes, String filename, String mimeType) throws IOException {
        if (!isAvailable()) {
            throw new IllegalStateException("Google Drive is not configured or not available");
        }

        File fileMetadata = new File();
        fileMetadata.setName(filename);
        fileMetadata.setParents(Collections.singletonList(folderId.trim()));

        InputStreamContent mediaContent = new InputStreamContent(
                mimeType != null ? mimeType : "application/octet-stream",
                new ByteArrayInputStream(bytes));
        mediaContent.setLength((long) bytes.length);

        File file = drive.files()
                .create(fileMetadata, mediaContent)
                .setFields("id")
                .execute();

        String fileId = file.getId();
        if (fileId == null || fileId.isBlank()) {
            throw new IOException("Drive API did not return file id");
        }

        // Allow anyone with the link to view
        Permission permission = new Permission();
        permission.setType("anyone");
        permission.setRole("reader");
        drive.permissions().create(fileId, permission).execute();

        String publicUrl = PUBLIC_VIEW_URL_PREFIX + fileId;
        log.debug("Uploaded to Drive: {} -> {}", filename, publicUrl);
        return publicUrl;
    }

    private GoogleCredentials loadCredentials() throws IOException {
        if (credentialsJson != null && !credentialsJson.isBlank()) {
            try (InputStream in = new ByteArrayInputStream(credentialsJson.getBytes(StandardCharsets.UTF_8))) {
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
}
