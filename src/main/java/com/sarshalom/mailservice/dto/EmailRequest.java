package com.sarshalom.mailservice.dto;

import java.util.List;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EmailRequest {

    @NotBlank
    @Email
    private String sender;

    @NotBlank
    @Email
    private String recipient;

    private String subject;
    private String body;
    private List<MultipartFile> attachments;
    private List<UUID> attachmentIds;
}
