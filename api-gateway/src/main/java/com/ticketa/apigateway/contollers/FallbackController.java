package com.ticketa.apigateway.contollers;

import com.ticketa.common.dto.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/events")
    public Mono<ApiErrorResponse> eventServiceFallback() {
        return Mono.just(new ApiErrorResponse(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                "Event Service is temporarily unavailable. Please try again later.",
                "/api/v1/events",
                LocalDateTime.now()
        ));
    }

    @GetMapping("/auth")
    public Mono<ApiErrorResponse> authServiceFallback() {
        return Mono.just(new ApiErrorResponse(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                "Authentication service is busy or down. Existing sessions are still active.",
                "/api/v1/auth",
                LocalDateTime.now()
        ));
    }
}