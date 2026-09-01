package com.sarshalom.mailservice.controller;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
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

import com.sarshalom.mailservice.dto.EmailRequest;
import com.sarshalom.mailservice.dto.EmailResponse;
import com.sarshalom.mailservice.model.Attachment;
import com.sarshalom.mailservice.service.AttachmentService;
import com.sarshalom.mailservice.service.EmailService;

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

    public EmailController(EmailService emailService, AttachmentService attachmentService) {
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

        log.info("Sending single email request; attachmentsCount={}",
                attachments != null ? attachments.size() : 0);

        EmailRequest emailRequest = new EmailRequest(sender, recipient, subject, body, attachments, null);
        EmailResponse response = emailService.sendEmail(emailRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(value = "/bulk", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<EmailResponse>> sendBulkEmails(
            @RequestPart("emailRequests") @Valid List<EmailRequest> emailRequests,
            HttpServletRequest request) {

        Map<String, MultipartFile> attachmentMapping = extractAttachmentsMapping(request);
        log.info("Sending bulk email request; emailRequestsCount={}, attachmentMapSize={}",
                emailRequests != null ? emailRequests.size() : 0,
                attachmentMapping.size());

        return ResponseEntity.ok(emailService.sendEmails(emailRequests, attachmentMapping));
    }

    private Map<String, MultipartFile> extractAttachmentsMapping(HttpServletRequest request) {
        Map<String, MultipartFile> attachmentMap = new HashMap<>();

        if (request instanceof MultipartHttpServletRequest multipartRequest) {
            multipartRequest.getFileNames().forEachRemaining(
                    name -> attachmentMap.put(name, multipartRequest.getFile(name)));
        }

        return attachmentMap;
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmailResponse> getEmail(@PathVariable Long id) {
        log.debug("Getting email by id={}", id);
        return ResponseEntity.ok(emailService.getEmailById(id));
    }

    @GetMapping("/search")
    public ResponseEntity<List<EmailResponse>> searchEmails(
            @RequestParam(required = false) String sender,
            @RequestParam(required = false) String recipient,
            @RequestParam(required = false) String subject) {

        log.debug("Searching emails; senderFilter={}, recipientFilter={}, subjectFilter={}",
                StringUtils.hasText(sender),
                StringUtils.hasText(recipient),
                StringUtils.hasText(subject));

        return ResponseEntity.ok(emailService.searchEmails(sender, recipient, subject));
    }

    @GetMapping("/sent")
    public ResponseEntity<List<EmailResponse>> getSentEmails() {
        return ResponseEntity.ok(emailService.getAllEmails());
    }

    @GetMapping("/attachments/{attachmentId}/download")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable Long attachmentId) {
        Attachment attachment = attachmentService.getAttachmentById(attachmentId);
        Resource resource = attachmentService.getAttachmentResource(attachment);
        return ResponseEntity.ok()
                .headers(createAttachmentHeaders(attachment))
                .body(resource);
    }

    private HttpHeaders createAttachmentHeaders(Attachment attachment) {
        HttpHeaders headers = new HttpHeaders();

        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(attachment.getFileName(), StandardCharsets.UTF_8)
                .build();
        headers.setContentDisposition(disposition);
        headers.setContentLength(attachment.getFileSize());

        if (StringUtils.hasText(attachment.getMimeType())) {
            try {
                headers.setContentType(MediaType.parseMediaType(attachment.getMimeType()));
            } catch (InvalidMediaTypeException ex) {
                headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            }
        } else {
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        }

        return headers;
    }
}
