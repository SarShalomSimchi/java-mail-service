package com.sarshalom.mailservice.dto;

import lombok.Builder;

@Builder
public record AttachmentResponse(
	  Long id,
	  String fileName,
	  String mimeType,
	  Long fileSize,
	  String downloadUrl // URL to download the attachment
) {}