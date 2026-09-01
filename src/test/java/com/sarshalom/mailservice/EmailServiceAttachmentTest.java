package com.sarshalom.mailservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.web.multipart.MultipartFile;

import com.sarshalom.mailservice.dto.EmailRequest;
import com.sarshalom.mailservice.mapper.EmailMapper;
import com.sarshalom.mailservice.model.Email;
import com.sarshalom.mailservice.repository.EmailRepository;
import com.sarshalom.mailservice.service.AttachmentService;
import com.sarshalom.mailservice.service.EmailService;

class EmailServiceAttachmentTest {
    @Mock
    private EmailRepository emailRepository;
    @Mock
    private AttachmentService attachmentService;
    @Spy
    private EmailMapper emailMapper = new EmailMapper();
    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void sendEmail_withAttachment_savesAndReturnsResponse() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("file.txt");
        when(file.getContentType()).thenReturn("text/plain");
        when(file.getSize()).thenReturn(123L);
        when(attachmentService.saveAttachment(file)).thenReturn("2025/08/11/file.txt");
        when(emailRepository.save(any(Email.class))).thenAnswer(invocation -> {
            Email savedEmail = invocation.getArgument(0);
            savedEmail.setId(2L);
            return savedEmail;
        });
        EmailRequest req = new EmailRequest("from@a.com", "to@b.com", "subj", "body", List.of(file), null);
        var resp = emailService.sendEmail(req);
        assertThat(resp).isNotNull();
    }

    @Test
    void sendBulkEmails_withNullRequests_throws() {
        assertThrows(IllegalArgumentException.class, () -> emailService.sendEmails(null, Collections.emptyMap()));
    }

    @Test
    void sendBulkEmails_withEmptyRequests_throws() {
        assertThrows(IllegalArgumentException.class, () -> emailService.sendEmails(Collections.emptyList(), Collections.emptyMap()));
    }
}
