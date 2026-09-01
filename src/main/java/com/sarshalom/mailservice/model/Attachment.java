package com.sarshalom.mailservice.model;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "attachments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Attachment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "email_id", nullable = false)
    private Email email;

    @NotNull
    @Column(name = "file_path")
    private String relativePath;

    @NotNull
    private String fileName;

    @NotNull
    private String mimeType;

    @Column(name = "file_size_bytes")
    private Long fileSize;
}
