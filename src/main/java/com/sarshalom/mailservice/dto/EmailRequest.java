package com.sarshalom.mailservice.dto;

import java.util.List;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@Valid
@AllArgsConstructor
public class EmailRequest{

	@NotNull @Email
	private String sender;
	@NotNull @Email
	private String recipient;
	private String subject;
	private String body;
	private List<MultipartFile> attachments;
	private List<UUID> attachmentIds;
}
