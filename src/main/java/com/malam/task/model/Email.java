package com.malam.task.model;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "email")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Email {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Sender cannot be null")
    @NotEmpty(message = "Sender cannot be empty")
    @jakarta.validation.constraints.Email(message = "Invalid email format for sender")
    private String sender;

    @NotNull(message = "Recipient cannot be null")
    @NotEmpty(message = "Recipient cannot be empty")
    @jakarta.validation.constraints.Email(message = "Invalid email format for recipient")
    private String recipient;
    private String subject;
    private String body;

    @Column(name = "create_date")
    private LocalDateTime createDate;

    // One email can have multiple attachments
    @OneToMany(mappedBy = "email", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Attachment> attachments;
}
