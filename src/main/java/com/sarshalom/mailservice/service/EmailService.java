package com.sarshalom.mailservice.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.sarshalom.mailservice.dto.EmailRequest;
import com.sarshalom.mailservice.dto.EmailResponse;
import com.sarshalom.mailservice.exception.ResourceNotFoundException;
import com.sarshalom.mailservice.mapper.EmailMapper;
import com.sarshalom.mailservice.model.Attachment;
import com.sarshalom.mailservice.model.Email;
import com.sarshalom.mailservice.repository.EmailRepository;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class EmailService {

    private static final Predicate<MultipartFile> VALID_ATTACHMENT =
            file -> file != null
                    && file.getSize() > 0
                    && file.getOriginalFilename() != null
                    && !file.getOriginalFilename().isBlank();

    private final EmailRepository emailRepository;
    private final AttachmentService attachmentService;
    private final EmailMapper emailMapper;

    public EmailService(
            EmailRepository emailRepository,
            AttachmentService attachmentService,
            EmailMapper emailMapper) {
        this.emailRepository = emailRepository;
        this.attachmentService = attachmentService;
        this.emailMapper = emailMapper;
    }

    public EmailResponse sendEmail(EmailRequest emailRequest) {
        int attachmentCount = emailRequest.getAttachments() == null
                ? 0
                : emailRequest.getAttachments().size();
        log.info("Persisting single email; attachmentsCount={}", attachmentCount);

        Email savedEmail = saveEmail(emailRequest);
        send(savedEmail);
        return emailMapper.toEmailResponse(savedEmail);
    }

    private Email saveEmail(EmailRequest emailRequest) {
        Email savedEmail = emailRepository.save(createEmail(emailRequest));
        log.debug("Persisted email id={}", savedEmail.getId());
        return savedEmail;
    }

    private Email createEmail(EmailRequest emailRequest) {
        Email email = emailMapper.toEmail(emailRequest);
        addAttachmentsToEmail(email, emailRequest);
        return email;
    }

    private void addAttachmentsToEmail(Email email, EmailRequest emailRequest) {
        if (hasAttachments(emailRequest)) {
            emailRequest.getAttachments().stream()
                    .filter(VALID_ATTACHMENT)
                    .map(file -> createAttachment(email, file))
                    .forEach(email.getAttachments()::add);
        }
    }

    private boolean hasAttachments(EmailRequest emailRequest) {
        return emailRequest.getAttachments() != null && !emailRequest.getAttachments().isEmpty();
    }

    private Attachment createAttachment(Email email, MultipartFile file) {
        String relativePath = attachmentService.saveAttachment(file);
        Attachment attachment = emailMapper.createAttachment(file, relativePath);
        attachment.setEmail(email);
        return attachment;
    }

    private void send(Email email) {
        log.debug("Email delivery stub invoked for email id={}", email.getId());
    }

    private void sendBulk(List<Email> emails) {
        log.debug("Bulk email delivery stub invoked; emailsCount={}", emails.size());
    }

    @Transactional
    public List<EmailResponse> sendEmails(
            List<EmailRequest> requests,
            Map<String, MultipartFile> attachmentMapping) {

        populateAttachments(requests, attachmentMapping);
        return persistAndSendAllEmails(requests);
    }

    private void populateAttachments(
            List<EmailRequest> requests,
            Map<String, MultipartFile> attachmentMapping) {

        if (requests != null) {
            requests.forEach(request ->
                    request.setAttachments(getAttachments(attachmentMapping, request)));
        }
    }

    private List<MultipartFile> getAttachments(
            Map<String, MultipartFile> attachmentMapping,
            EmailRequest request) {

        return request.getAttachmentIds() != null
                ? request.getAttachmentIds().stream()
                        .map(attachmentId -> getFile(attachmentMapping, attachmentId))
                        .toList()
                : new ArrayList<>();
    }

    private MultipartFile getFile(
            Map<String, MultipartFile> attachmentMapping,
            UUID attachmentId) {

        MultipartFile file = attachmentMapping.get(attachmentId.toString());
        return Optional.ofNullable(file)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Attachment not found in request: " + attachmentId));
    }

    private List<EmailResponse> persistAndSendAllEmails(List<EmailRequest> emailRequests) {
        if (emailRequests == null || emailRequests.isEmpty()) {
            throw new IllegalArgumentException("Email requests cannot be null or empty");
        }

        log.info("Persisting bulk emails; emailRequestsCount={}", emailRequests.size());

        List<Email> emails = emailRequests.stream()
                .map(this::createEmail)
                .toList();

        List<Email> savedEmails = emailRepository.saveAll(emails);
        sendBulk(savedEmails);

        return savedEmails.stream()
                .map(emailMapper::toEmailResponse)
                .toList();
    }

    public EmailResponse getEmailById(Long id) {
        return emailRepository.findById(id)
                .map(emailMapper::toEmailResponse)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Email not found with ID: " + id));
    }

    public List<EmailResponse> searchEmails(
            String sender,
            String recipient,
            String subject) {

        List<Email> emails = emailRepository.searchEmails(
                nullIfEmpty(sender),
                nullIfEmpty(recipient),
                nullIfEmpty(subject));

        return emails.stream()
                .map(emailMapper::toEmailResponse)
                .toList();
    }

    private String nullIfEmpty(String value) {
        return value != null && !value.isEmpty() ? value : null;
    }

    public List<EmailResponse> getAllEmails() {
        return emailRepository.findAll().stream()
                .map(emailMapper::toEmailResponse)
                .toList();
    }
}
