package com.alice.smartteacher.service;

import com.alice.smartteacher.web.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;

@Service
public class FileStore {

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    public Path root() {
        try {
            Path path = Path.of(uploadDir);
            if (!path.isAbsolute()) {
                path = Path.of(System.getProperty("user.dir")).resolve(path);
            }
            Files.createDirectories(path);
            return path.toAbsolutePath().normalize();
        } catch (IOException ex) {
            throw new ApiException(500, "Could not open the uploads folder.");
        }
    }

    public Stored save(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(400, "Choose a file first.");
        }
        String original = file.getOriginalFilename() == null ? "file" : Path.of(file.getOriginalFilename()).getFileName().toString();
        String ext = extension(original);
        String storedName = UUID.randomUUID() + ext;
        try {
            file.transferTo(root().resolve(storedName));
        } catch (IOException ex) {
            throw new ApiException(500, "Could not save the file.");
        }
        String type = file.getContentType() == null ? "application/octet-stream" : file.getContentType();
        return new Stored(storedName, original, type);
    }

    public String saveText(String originalName, String text) {
        String ext = extension(originalName);
        String storedName = UUID.randomUUID() + (ext.isEmpty() ? ".txt" : ext);
        try {
            Files.writeString(root().resolve(storedName), text, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new ApiException(500, "Could not save the file.");
        }
        return storedName;
    }

    public String saveBytes(String originalName, byte[] bytes) {
        String ext = extension(originalName);
        String storedName = UUID.randomUUID() + ext;
        try {
            Files.write(root().resolve(storedName), bytes);
        } catch (IOException ex) {
            throw new ApiException(500, "Could not save the file.");
        }
        return storedName;
    }

    public Path resolve(String storedName) {
        if (storedName == null || storedName.contains("..") || storedName.contains("/") || storedName.contains("\\")) {
            throw new ApiException(400, "Unknown file.");
        }
        Path path = root().resolve(storedName).normalize();
        if (!path.startsWith(root()) || !Files.exists(path)) {
            throw new ApiException(404, "File not found.");
        }
        return path;
    }

    public long folderBytes() {
        try {
            return Files.walk(root())
                    .filter(Files::isRegularFile)
                    .mapToLong(p -> {
                        try {
                            return Files.size(p);
                        } catch (IOException e) {
                            return 0;
                        }
                    }).sum();
        } catch (IOException ex) {
            return 0;
        }
    }

    private String extension(String name) {
        int dot = name.lastIndexOf('.');
        if (dot < 0) {
            return "";
        }
        String ext = name.substring(dot).toLowerCase(Locale.ROOT);
        if (ext.length() > 8) {
            return "";
        }
        return ext;
    }

    public static class Stored {
        public final String storedName;
        public final String originalName;
        public final String contentType;

        public Stored(String storedName, String originalName, String contentType) {
            this.storedName = storedName;
            this.originalName = originalName;
            this.contentType = contentType;
        }
    }
}
