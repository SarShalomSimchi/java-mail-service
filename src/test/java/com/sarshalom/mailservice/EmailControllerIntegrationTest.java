package com.sarshalom.mailservice;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

import com.sarshalom.mailservice.model.Attachment;
import com.sarshalom.mailservice.repository.AttachmentRepository;
import com.sarshalom.mailservice.repository.EmailRepository;
import com.sarshalom.mailservice.service.AttachmentService;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(
		properties = "storage.attachments.basePath=target/test-attachments"
)
class EmailControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AttachmentRepository attachmentRepository;
    @Autowired
    private EmailRepository emailRepository;
    @Autowired
    private AttachmentService attachmentService;


    @Test
    @DisplayName("Send single email with attachment")
    void sendEmail_withAttachment() throws Exception {
        MockMultipartFile sender = new MockMultipartFile(
                "sender",
                "",
                MediaType.TEXT_PLAIN_VALUE,
                "sender@example.com".getBytes(StandardCharsets.UTF_8)
        );

        MockMultipartFile recipient = new MockMultipartFile(
                "recipient",
                "",
                MediaType.TEXT_PLAIN_VALUE,
                "recipient@example.com".getBytes(StandardCharsets.UTF_8)
        );

        MockMultipartFile subject = new MockMultipartFile(
                "subject",
                "",
                MediaType.TEXT_PLAIN_VALUE,
                "Test Subject".getBytes(StandardCharsets.UTF_8)
        );

        MockMultipartFile body = new MockMultipartFile(
                "body",
                "",
                MediaType.TEXT_PLAIN_VALUE,
                "Test Body".getBytes(StandardCharsets.UTF_8)
        );

        MockMultipartFile attachment = new MockMultipartFile(
                "attachments",
                "test.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "hello".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/emails/send")
                .file(sender)
                .file(recipient)
                .file(subject)
                .file(body)
                .file(attachment))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sender", is("sender@example.com")))
                .andExpect(jsonPath("$.attachments", hasSize(1)));
    }

    @Test
    @DisplayName("Send bulk emails")
    void sendBulkEmails() throws Exception {
        String emailRequestsJson = "[" +
                "{\"sender\":\"bulk1@example.com\",\"recipient\":\"r1@example.com\",\"subject\":\"S1\",\"body\":\"B1\",\"attachments\":[],\"attachmentIds\":null}," +
                "{\"sender\":\"bulk2@example.com\",\"recipient\":\"r2@example.com\",\"subject\":\"S2\",\"body\":\"B2\",\"attachments\":[],\"attachmentIds\":null}"+"]";
        MockMultipartFile emailRequests = new MockMultipartFile("emailRequests", "emailRequests.json", MediaType.APPLICATION_JSON_VALUE, emailRequestsJson.getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/emails/bulk")
                .file(emailRequests))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("Search emails")
    void searchEmails() throws Exception {
        // Insert a test email
        com.sarshalom.mailservice.model.Email email = new com.sarshalom.mailservice.model.Email();
        email.setSender("sender@search.com");
        email.setRecipient("recipient@search.com");
        email.setSubject("Search Subject");
        email.setBody("Search Body");
        email.setAttachments(List.of());
        emailRepository.save(email);
        mockMvc.perform(get("/api/emails/search")
                .param("sender", "sender@search.com")
                .param("recipient", "recipient@search.com")
                .param("subject", "Search Subject"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Get all sent emails")
    void getAllSentEmails() throws Exception {
        mockMvc.perform(get("/api/emails/sent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", isA(List.class)));
    }

    @Test
    @DisplayName("Download attachment")
    void downloadAttachment() throws Exception {
        com.sarshalom.mailservice.model.Email email = new com.sarshalom.mailservice.model.Email();
        email.setSender("sender@example.com");
        email.setRecipient("recipient@example.com");
        email.setSubject("Test Subject");
        email.setBody("Test Body");
        email.setAttachments(List.of());

        email = emailRepository.save(email);

        MockMultipartFile file = new MockMultipartFile(
                "attachments",
                "test.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "hello".getBytes(StandardCharsets.UTF_8)
        );

        String relativePath = attachmentService.saveAttachment(file);

        Attachment att = new Attachment();
        att.setEmail(email);
        att.setFileName("test.txt");
        att.setMimeType("text/plain");
        att.setFileSize(5L);
        att.setRelativePath(relativePath);

        att = attachmentRepository.save(att);

        mockMvc.perform(get("/api/emails/attachments/" + att.getId() + "/download"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "Content-Disposition",
                        containsString("test.txt")
                ));
    }
}
