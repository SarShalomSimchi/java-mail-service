package com.malam.task.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;

@Builder
public record EmailResponse(
	    Long id,
	    String sender,
	    String recipient,
	    String subject,
	    String body,
	    LocalDateTime createDate,
	    List<AttachmentResponse> attachments
	) {}
