package com.ticketa.common.dto;
import java.time.LocalDateTime;

public record ApiErrorResponse(
        int status,
        String message,
        String path,
        LocalDateTime timestamp
) {}