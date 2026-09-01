package com.sarshalom.mailservice.dto;

import java.time.LocalDateTime;

import lombok.Builder;

@Builder
public record ErrorResponse(
	    LocalDateTime timestamp,
	    int status,
	    String error,
	    String message, // Exception message
	    String path // Request path
) {}
