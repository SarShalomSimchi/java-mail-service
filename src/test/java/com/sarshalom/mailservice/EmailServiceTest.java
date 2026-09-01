package com.sarshalom.mailservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import com.sarshalom.mailservice.dto.EmailRequest;
import com.sarshalom.mailservice.mapper.EmailMapper;
import com.sarshalom.mailservice.model.Email;
import com.sarshalom.mailservice.repository.EmailRepository;
import com.sarshalom.mailservice.service.AttachmentService;
import com.sarshalom.mailservice.service.EmailService;

class EmailServiceTest {
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
    void sendEmail_savesAndReturnsResponse() {
        EmailRequest req = new EmailRequest("from@a.com", "to@b.com", "subj", "body", Collections.emptyList(), null);
        when(emailRepository.save(any(Email.class))).thenAnswer(invocation -> {
            Email email = invocation.getArgument(0);
            email.setId(1L);
            return email;
        });
        var resp = emailService.sendEmail(req);
        assertThat(resp).isNotNull();
        assertThat(resp.id()).isEqualTo(1L);
    }

    @Test
    void getEmailById_notFound_throws() {
        when(emailRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> emailService.getEmailById(1L));
    }
}