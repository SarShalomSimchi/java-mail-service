package com.malam.task.mapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.malam.task.dto.AttachmentResponse;
import com.malam.task.dto.EmailRequest;
import com.malam.task.dto.EmailResponse;
import com.malam.task.model.Attachment;
import com.malam.task.model.Email;

@Component
public class EmailMapper {

    private static final String ATTACHMENT_DOWNLOAD_URL_TEMPLATE = "/api/emails/attachments/%d/download";

    public Email toEmail(EmailRequest emailRequest) {
        Email email = new Email();
        BeanUtils.copyProperties(emailRequest, email);
        email.setCreateDate(LocalDateTime.now());
        email.setAttachments(new ArrayList<>());
        return email;
    }

    public EmailResponse toEmailResponse(Email email) {
        List<AttachmentResponse> attachmentResponses = email.getAttachments().stream()
                .map(this::toAttachmentResponse)
                .toList();

        return EmailResponse.builder()
                .id(email.getId())
                .sender(email.getSender())
                .recipient(email.getRecipient())
                .subject(email.getSubject())
                .body(email.getBody())
                .createDate(email.getCreateDate())
                .attachments(attachmentResponses)
                .build();
    }

    public Attachment createAttachment(MultipartFile file, String relativePath) {
        Attachment attachment = new Attachment();
        attachment.setFileName(file.getOriginalFilename());
        attachment.setMimeType(file.getContentType());
        attachment.setFileSize(file.getSize());
        attachment.setRelativePath(relativePath);
        return attachment;
    }

    public AttachmentResponse toAttachmentResponse(Attachment attachment) {
        return AttachmentResponse.builder()
                .id(attachment.getId())
                .fileName(attachment.getFileName())
                .mimeType(attachment.getMimeType())
                .fileSize(attachment.getFileSize())
                .downloadUrl(String.format(ATTACHMENT_DOWNLOAD_URL_TEMPLATE, attachment.getId()))
                .build();
    }
}
