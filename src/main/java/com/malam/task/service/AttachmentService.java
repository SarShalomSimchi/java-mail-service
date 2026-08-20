package com.malam.task.service;


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

import com.malam.task.exception.FileStorageException;
import com.malam.task.exception.ResourceNotFoundException;
import com.malam.task.model.Attachment;
import com.malam.task.repository.AttachmentRepository;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class AttachmentService {

    @Value("${storage.attachments.basePath}")
    private String attachmentBasePath;

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final AttachmentRepository attachmentRepository;

    public AttachmentService(AttachmentRepository attachmentRepository) {
        this.attachmentRepository = attachmentRepository;
    }

    /**
     * Saves a MultipartFile to the configured attachment base path.
     * The file is stored in a time-based directory structure (year/month/day)
     * and named with a unique UUID.
     *
     * @param file The MultipartFile to save.
     * @return The relative path of the saved file (e.g., '2025/08/04/uuid.pdf').
     * @throws FileStorageException If an I/O error occurs during file saving.
     */
    public String saveAttachment(MultipartFile file) {
        log.info("AttachmentService - saveAttachment: fileName={}, size={}, contentType={}", file.getOriginalFilename(), file.getSize(), file.getContentType());
        try {
            String datePath = LocalDateTime.now().format(dateFormatter);

            Path directoryPath = Paths.get(attachmentBasePath, datePath);
            Files.createDirectories(directoryPath);

            String uniqueFilename = createUniqueFilename(file);

            Path filePath = directoryPath.resolve(uniqueFilename);
            log.info("AttachmentService - saveAttachment: filePath={}", filePath);

            Files.copy(file.getInputStream(), filePath);

            return datePath + "/" + uniqueFilename;
        } catch (IOException ex) {
            log.error("AttachmentService - saveAttachment: Failed to store file: {}. Exception: {}", file.getOriginalFilename(), ex.getMessage(), ex);
            throw new FileStorageException("Could not store file " + file.getOriginalFilename() + ". Please try again!", ex);
        }
    }

	private String createUniqueFilename(MultipartFile file) {
		String fileExtension = getFileExtension(file);
		return UUID.randomUUID().toString() + fileExtension;
	}

	private String getFileExtension(MultipartFile file) {
		String originalFilename = file.getOriginalFilename();
		String fileExtension = "";
		if (originalFilename != null && originalFilename.contains(".")) {
		    fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
		}
		return fileExtension;
	}

	public Resource getAttachmentResource(Attachment attachment) {
        log.info("AttachmentService - getAttachmentResource: attachmentId={}, relativePath={}", attachment.getId(), attachment.getRelativePath());
        return loadAttachment(attachment.getRelativePath());
    }

    public Attachment getAttachmentById(Long attachmentId) {
        log.info("AttachmentService - getAttachmentById: attachmentId={}", attachmentId);
        return attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> {
                    log.error("AttachmentService - getAttachmentById: Attachment not found with ID: {}", attachmentId);
                    return new ResourceNotFoundException("Attachment not found with ID: " + attachmentId);
                });
    }

    private Resource loadAttachment(String relativePath) {
        log.info("AttachmentService - loadAttachment: relativePath={}", relativePath);
        Path filePath = getNormalizedFilePath(relativePath);
        Resource resource = getResource(filePath);
        validateResource(relativePath, resource);
        return resource;
    }

    private Path getNormalizedFilePath(String relativePath) {
        log.info("AttachmentService - getNormalizedFilePath: relativePath={}", relativePath);
        return Paths.get(attachmentBasePath).resolve(relativePath).normalize();
    }

    private Resource getResource(Path filePath) {
        log.info("AttachmentService - getResource: filePath={}", filePath);
        try {
            return new UrlResource(filePath.toUri());
        } catch (MalformedURLException ex) {
            log.error("AttachmentService - getResource: Invalid file path: {}. Exception: {}", filePath, ex.getMessage(), ex);
            throw new FileStorageException("File path is invalid: " + filePath, ex);
        }
    }

    private void validateResource(String relativePath, Resource resource) {
        if (!resource.exists() || !resource.isReadable()) {
            log.error("AttachmentService - validateResource: File not found or not readable: {}", relativePath);
            throw new FileStorageException("File not found or not readable: " + relativePath);
        }
    }



}
