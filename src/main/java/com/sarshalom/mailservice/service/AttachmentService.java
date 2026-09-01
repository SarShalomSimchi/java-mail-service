package com.sarshalom.mailservice.service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.sarshalom.mailservice.exception.FileStorageException;
import com.sarshalom.mailservice.exception.ResourceNotFoundException;
import com.sarshalom.mailservice.model.Attachment;
import com.sarshalom.mailservice.repository.AttachmentRepository;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class AttachmentService {

    @Value("${storage.attachments.basePath}")
    private String attachmentBasePath;

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final AttachmentRepository attachmentRepository;

    public AttachmentService(AttachmentRepository attachmentRepository) {
        this.attachmentRepository = attachmentRepository;
    }

    public String saveAttachment(MultipartFile file) {
        log.debug("Saving attachment; size={}, contentType={}",
                file.getSize(), file.getContentType());

        try {
            String datePath = LocalDateTime.now().format(DATE_FORMATTER);
            Path directoryPath = getBasePath().resolve(datePath);
            Files.createDirectories(directoryPath);

            String uniqueFilename = createUniqueFilename(file);
            Path filePath = directoryPath.resolve(uniqueFilename);
            Files.copy(file.getInputStream(), filePath);

            return datePath + "/" + uniqueFilename;
        } catch (IOException ex) {
            log.error("Failed to store attachment", ex);
            throw new FileStorageException("Could not store attachment.", ex);
        }
    }

    private String createUniqueFilename(MultipartFile file) {
        return UUID.randomUUID() + getFileExtension(file);
    }

    private String getFileExtension(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && originalFilename.contains(".")) {
            return originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return "";
    }

    public Resource getAttachmentResource(Attachment attachment) {
        return loadAttachment(attachment.getRelativePath());
    }

    public Attachment getAttachmentById(Long attachmentId) {
        return attachmentRepository.findById(attachmentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Attachment not found with ID: " + attachmentId));
    }

    private Resource loadAttachment(String relativePath) {
        Path filePath = resolveSafePath(relativePath);

        try {
            Resource resource = new UrlResource(filePath.toUri());
            validateResource(resource);
            return resource;
        } catch (MalformedURLException ex) {
            throw new FileStorageException("Attachment path is invalid.", ex);
        }
    }

    private Path resolveSafePath(String relativePath) {
        Path basePath = getBasePath();
        Path resolvedPath = basePath.resolve(relativePath).normalize();

        if (!resolvedPath.startsWith(basePath)) {
            throw new FileStorageException("Attachment path is outside the configured storage directory.");
        }

        return resolvedPath;
    }

    private Path getBasePath() {
        return Paths.get(attachmentBasePath).toAbsolutePath().normalize();
    }

    private void validateResource(Resource resource) {
        if (!resource.exists() || !resource.isReadable()) {
            throw new FileStorageException("Attachment file was not found or is not readable.");
        }
    }
}
