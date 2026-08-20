package com.malam.task.service;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.malam.task.dto.EmailRequest;
import com.malam.task.dto.EmailResponse;
import com.malam.task.exception.ResourceNotFoundException;
import com.malam.task.mapper.EmailMapper;
import com.malam.task.model.Attachment;
import com.malam.task.model.Email;
import com.malam.task.repository.EmailRepository;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class EmailService {
	private static final Predicate<MultipartFile> VALID_ATTACHMENT =
	        file -> file != null && file.getSize() > 0
	        && file.getOriginalFilename() != null
	        && !file.getOriginalFilename().isBlank();

    private final EmailRepository emailRepository;
    private final AttachmentService attachmentService;
    private final EmailMapper emailMapper;

    public EmailService(EmailRepository emailRepository,
                        AttachmentService attachmentService,
                        EmailMapper emailMapper) {
        this.emailRepository = emailRepository;
        this.attachmentService = attachmentService;
        this.emailMapper = emailMapper;
    }

    public EmailResponse sendEmail(EmailRequest emailRequest) {
        log.info("EmailService - savesAndSendEmail: emailRequest={}", emailRequest);
        Email savedEmail = saveEmail(emailRequest);
        if (savedEmail == null) {
            throw new IllegalStateException("Failed to save email");
        }
        send(savedEmail);
        return emailMapper.toEmailResponse(savedEmail);
    }

    private Email saveEmail(EmailRequest emailRequest) {
        Email email = createEmail(emailRequest);
        Email savedEmail = emailRepository.save(email);
        log.info("EmailService - sendEmail: savedEmailId={}", savedEmail.getId());
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
                .map(file -> createAndSaveAttachment(email, file))
                .forEach(email.getAttachments()::add);
        }
    }

    private boolean hasAttachments(EmailRequest emailRequest) {
	return emailRequest.getAttachments() != null && !emailRequest.getAttachments().isEmpty();
    }

    private Attachment createAndSaveAttachment(Email email, MultipartFile file) {
        String relativePath = attachmentService.saveAttachment(file);
        Attachment attachment = emailMapper.createAttachment(file, relativePath);
        attachment.setEmail(email);
        return attachment;
    }

    private void send(Email email) {
		log.info("EmailService - send: emailId={}", email.getId());
		log.info("EmailService - send: start sending email....");
        //send mail logic can be added here, e.g., using JavaMailSender....
		// This is a placeholder for actual email sending logic
		log.info("EmailService - send: finished sending email....");
		// Logic to send the email, e.g., using JavaMailSender
	}

    private void sendBulk(List<Email> emails) {
		log.info("EmailService - sendBulk: emailsCount={}", emails.size());
		log.info("EmailService - sendBulk: start sending emails....");
	    //send mail logic can be added here, e.g., using JavaMailSender....
		// This is a placeholder for actual email sending logic
	    log.info("EmailService - sendBulk: finished sending emails....");
	}


    @Transactional
    public List<EmailResponse> sendEmails(List<EmailRequest> requests,
            Map<String, MultipartFile> attachmentMapping) {
        populateAttachments(requests, attachmentMapping);
        return persistAndSendAllEmails(requests);
    }

    private void populateAttachments(List<EmailRequest> requests, Map<String, MultipartFile> attachmentMapping) {
        if (requests != null) {
            requests.forEach(request ->
                request.setAttachments(getAttachments(attachmentMapping, request))
            );
        }
    }

	private List<MultipartFile> getAttachments(Map<String, MultipartFile> attachmentMapping, EmailRequest request) {
		return request.getAttachmentIds() != null
		        ? request.getAttachmentIds().stream()
		            .map(attachmentId ->  getFile(attachmentMapping, attachmentId))
		            .toList()
		        : new ArrayList<>();
	}


	private MultipartFile getFile(Map<String, MultipartFile> attachmentMapping, UUID attachmentId) {
		MultipartFile file = attachmentMapping.get(attachmentId.toString());
		return Optional.ofNullable(file)
		        .orElseThrow(() -> new ResourceNotFoundException("Attachment not found in request: " + attachmentId));
	}

	private List<EmailResponse> persistAndSendAllEmails(List<EmailRequest> emailRequests) {
		log.info("EmailService - persistAndSendAllEmails (bulk): emailRequestsCount={}", emailRequests != null ? emailRequests.size() : 0);
		if (emailRequests == null || emailRequests.isEmpty()) {
			throw new IllegalArgumentException("Email requests cannot be null or empty");
		}

	    List<Email> emails = emailRequests.stream()
	        .map(this::createEmail)
	        .toList();

	    List<Email> savedEmails = emailRepository.saveAll(emails);
	    sendBulk(savedEmails);

	    return savedEmails.stream().map(emailMapper::toEmailResponse).toList();
	}


    public EmailResponse getEmailById(Long id) {
        log.info("EmailService - getEmailById: id={}", id);
        return emailRepository.findById(id)
                .map(emailMapper::toEmailResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Email not found with ID: " + id));
    }

    public List<EmailResponse> searchEmails(String sender, String recipient, String subject) {
        log.info("EmailService - searchEmails: sender={}, recipient={}, subject={}", sender, recipient, subject);
        String searchSender = nullIfEmpty(sender);
        String searchRecipient = nullIfEmpty(recipient);
        String searchSubject = nullIfEmpty(subject);
        List<Email> emails = emailRepository.searchEmails(searchSender, searchRecipient, searchSubject);
        return emails.stream()
                .map(emailMapper::toEmailResponse)
                .toList();
    }

    private String nullIfEmpty(String value) {
        return (value != null && !value.isEmpty()) ? value : null;
    }

    public List<EmailResponse> getAllEmails() {
        log.info("EmailService - getAllEmails");
        List<Email> emails = emailRepository.findAll();
        return emails.stream()
                .map(emailMapper::toEmailResponse)
                .toList();
    }
}
