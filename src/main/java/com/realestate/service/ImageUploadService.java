package com.realestate.service;

import com.realestate.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import javax.imageio.ImageIO;

@Service
@Slf4j
@RequiredArgsConstructor
public class ImageUploadService {

    private static final List<String> ALLOWED_TYPES = Arrays.asList("image/jpeg", "image/png", "image/webp", "image/gif");
    private static final long MAX_SIZE_BYTES = 10 * 1024 * 1024; // 10 MB
    private static final int MAX_WIDTH = 1200;
    private static final int MAX_HEIGHT = 1200;
    private static final int THUMB_WIDTH = 300;
    private static final int THUMB_HEIGHT = 300;
    private static final double JPEG_QUALITY = 0.85;

    private final GoogleDriveUploadService googleDriveUploadService;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    public ImageUploadResult upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("No file provided");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException("Invalid image type. Allowed: JPEG, PNG, WebP, GIF");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new BadRequestException("File too large. Max 10 MB");
        }
        try {
            String ext = getExtension(contentType);
            String baseName = UUID.randomUUID().toString().replace("-", "");
            String format = ext.replace(".", "");
            if (format.isEmpty()) format = "jpg";

            BufferedImage original;
            try (InputStream in = file.getInputStream()) {
                original = ImageIO.read(in);
            }
            if (original == null) {
                throw new BadRequestException("Could not read image");
            }

            if (googleDriveUploadService.isAvailable()) {
                return uploadToGoogleDrive(original, baseName, ext, format, contentType);
            }
            return uploadToLocal(original, baseName, ext, format);
        } catch (IOException e) {
            log.warn("Image upload failed", e);
            throw new BadRequestException("Failed to process image: " + e.getMessage());
        }
    }

    private ImageUploadResult uploadToGoogleDrive(BufferedImage original, String baseName, String ext, String format, String contentType) throws IOException {
        String mainName = baseName + "_main" + ext;
        String thumbName = baseName + "_thumb" + ext;

        ByteArrayOutputStream mainOut = new ByteArrayOutputStream();
        Thumbnails.of(original)
                .size(MAX_WIDTH, MAX_HEIGHT)
                .outputFormat(format)
                .outputQuality(JPEG_QUALITY)
                .toOutputStream(mainOut);
        byte[] mainBytes = mainOut.toByteArray();

        ByteArrayOutputStream thumbOut = new ByteArrayOutputStream();
        Thumbnails.of(original)
                .size(THUMB_WIDTH, THUMB_HEIGHT)
                .outputFormat(format)
                .outputQuality(0.8)
                .toOutputStream(thumbOut);
        byte[] thumbBytes = thumbOut.toByteArray();

        String mimeType = getMimeType(contentType);
        String mainUrl = googleDriveUploadService.upload(mainBytes, mainName, mimeType);
        String thumbUrl = googleDriveUploadService.upload(thumbBytes, thumbName, mimeType);

        return new ImageUploadResult(mainUrl, thumbUrl);
    }

    private ImageUploadResult uploadToLocal(BufferedImage original, String baseName, String ext, String format) throws IOException {
        Path base = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(base);
        String mainName = baseName + "_main" + ext;
        String thumbName = baseName + "_thumb" + ext;
        Path mainPath = base.resolve(mainName);
        Path thumbPath = base.resolve(thumbName);

        Thumbnails.of(original)
                .size(MAX_WIDTH, MAX_HEIGHT)
                .outputFormat(format)
                .outputQuality(JPEG_QUALITY)
                .toFile(mainPath.toFile());

        Thumbnails.of(original)
                .size(THUMB_WIDTH, THUMB_HEIGHT)
                .outputFormat(format)
                .outputQuality(0.8)
                .toFile(thumbPath.toFile());

        String urlPath = "/uploads/" + mainName;
        String thumbPathUrl = "/uploads/" + thumbName;
        return new ImageUploadResult(urlPath, thumbPathUrl);
    }

    private static String getMimeType(String contentType) {
        if (contentType == null) return "image/jpeg";
        return contentType.split(";")[0].trim();
    }

    private static String getExtension(String contentType) {
        if (contentType == null) return ".jpg";
        return switch (contentType.toLowerCase().split(";")[0].trim()) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> ".jpg";
        };
    }

    public record ImageUploadResult(String url, String thumbnailUrl) {}
}
