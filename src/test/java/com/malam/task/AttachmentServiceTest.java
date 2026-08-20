package com.malam.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import com.malam.task.exception.FileStorageException;
import com.malam.task.model.Attachment;
import com.malam.task.repository.AttachmentRepository;
import com.malam.task.service.AttachmentService;

class AttachmentServiceTest {
    @Mock
    private AttachmentRepository attachmentRepository;
    @InjectMocks
    private AttachmentService attachmentService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(
                attachmentService,
                "attachmentBasePath",
                "target/test-attachments"
        );
    }

    @Test
    void getAttachmentById_found() {
        Attachment att = new Attachment();
        att.setId(1L);
        when(attachmentRepository.findById(1L)).thenReturn(Optional.of(att));
        Attachment result = attachmentService.getAttachmentById(1L);
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void getAttachmentById_notFound_throws() {
        when(attachmentRepository.findById(2L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> attachmentService.getAttachmentById(2L));
    }

    @Test
    void getAttachmentResource_invalidPath_throws() {
        Attachment att = new Attachment();
        att.setRelativePath("missing-file.txt");
        assertThrows(FileStorageException.class, () -> attachmentService.getAttachmentResource(att));
    }
}
