package com.malam.task.controller;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import com.malam.task.dto.EmailRequest;
import com.malam.task.dto.EmailResponse;
import com.malam.task.model.Attachment;
import com.malam.task.service.AttachmentService;
import com.malam.task.service.EmailService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.log4j.Log4j2;

@RestController
@Log4j2
@Validated
@RequestMapping("/api/emails")
public class EmailController {

    private final EmailService emailService;
    private final AttachmentService attachmentService;

    public EmailController(EmailService emailService,
                           AttachmentService attachmentService) {
        this.emailService = emailService;
        this.attachmentService = attachmentService;
    }

    @PostMapping(value = "/send", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EmailResponse> sendEmailMultipart(
            @RequestPart("sender") @NotBlank @Email String sender,
            @RequestPart("recipient") @NotBlank @Email String recipient,
            @RequestPart("subject") String subject,
            @RequestPart("body") String body,
            @RequestPart(value = "attachments", required = false) List<MultipartFile> attachments) {
        log.info("EmailController - sendEmailMultipart: sender={}, recipient={}, subject={}, body={}, attachmentsCount={}", sender, recipient, subject, body, attachments != null ? attachments.size() : 0);
        EmailRequest emailRequest = new EmailRequest(sender, recipient, subject, body, attachments, null);
        EmailResponse response = emailService.sendEmail(emailRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(value = "/bulk", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<EmailResponse>> sendBulkEmails(
            @RequestPart("emailRequests") @Valid List<EmailRequest> emailRequests,
            HttpServletRequest request) {

        Map<String, MultipartFile> attachmentMapping = extractAttachmentsMapping(request);

        log.info("EmailController - sendBulkEmails: emailRequestsCount={}, attachmentMapSize={}",
				 emailRequests != null ? emailRequests.size() : 0, attachmentMapping.size());

        List<EmailResponse> results = emailService.sendEmails(emailRequests, attachmentMapping);
        return ResponseEntity.ok(results);
    }

    private Map<String, MultipartFile> extractAttachmentsMapping(HttpServletRequest request) {
        Map<String, MultipartFile> attachmentMap = new HashMap<>();

        if (request instanceof MultipartHttpServletRequest mreq) {
            mreq.getFileNames().forEachRemaining(name ->
                attachmentMap.put(name, mreq.getFile(name))
            );
        }

        return attachmentMap;
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmailResponse> getEmail(@PathVariable Long id) {
        log.info("EmailController - getEmail: id={}", id);
        EmailResponse email = emailService.getEmailById(id);
        return ResponseEntity.ok(email);
    }

    @GetMapping("/search")
    public ResponseEntity<List<EmailResponse>> searchEmails(
            @RequestParam(required = false) String sender,
            @RequestParam(required = false) String recipient,
            @RequestParam(required = false) String subject) {
        log.info("EmailController - searchEmails: sender={}, recipient={}, subject={}", sender, recipient, subject);
        List<EmailResponse> emails = emailService.searchEmails(sender, recipient, subject);
        log.info("EmailController - searchEmails: emailsCount={}", emails != null ? emails.size() : 0);
        return ResponseEntity.ok(emails);
    }

    @GetMapping("/sent")
    public ResponseEntity<List<EmailResponse>> getSentEmails() {
        log.info("EmailController - getSentEmails");
        List<EmailResponse> emails = emailService.getAllEmails();
        return ResponseEntity.ok(emails);
    }

    @GetMapping("/attachments/{attachmentId}/download")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable Long attachmentId) {
        log.info("EmailController - downloadAttachment: attachmentId={}", attachmentId);
        Attachment attachment = attachmentService.getAttachmentById(attachmentId);
        Resource resource = attachmentService.getAttachmentResource(attachment);
        HttpHeaders headers = createAttachmentHeaders(attachment);
        return ResponseEntity.ok()
                .headers(headers)
                .body(resource);
    }

    private HttpHeaders createAttachmentHeaders(Attachment attachment) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + attachment.getFileName() + "\"");
        headers.add(HttpHeaders.CONTENT_TYPE, attachment.getMimeType());
        headers.add(HttpHeaders.CONTENT_LENGTH, String.valueOf(attachment.getFileSize()));
        return headers;
    }
}
