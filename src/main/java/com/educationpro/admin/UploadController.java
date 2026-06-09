package com.educationpro.admin;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/admin/upload")
@PreAuthorize("hasRole('ADMIN')")
public class UploadController {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    private static final long MAX_BYTES = 5 * 1024 * 1024; // 5 MB

    @PostMapping
    public ResponseEntity<Map<String, String>> upload(@RequestParam("file") MultipartFile file)
            throws IOException {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Empty file"));
        }
        if (file.getSize() > MAX_BYTES) {
            return ResponseEntity.badRequest().body(Map.of("error", "File exceeds 5 MB limit"));
        }

        String original  = file.getOriginalFilename();
        String ext       = (original != null && original.contains("."))
                           ? original.substring(original.lastIndexOf('.'))
                           : ".png";
        String filename  = UUID.randomUUID() + ext;

        Path dest = Paths.get(uploadDir, "questions").toAbsolutePath();
        Files.createDirectories(dest);
        file.transferTo(dest.resolve(filename));

        String path = "/uploads/questions/" + filename;
        log.info("Uploaded image: {}", path);
        return ResponseEntity.ok(Map.of("path", path));
    }
}
