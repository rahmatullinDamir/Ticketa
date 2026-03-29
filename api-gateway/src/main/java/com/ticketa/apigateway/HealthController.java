package com.ticketa.apigateway;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health")
public class HealthController {

    @GetMapping
    public String check() {
        return "✅ " +
                System.getenv("SPRING_APPLICATION_NAME") +
                " is UP and running! " +
                java.time.LocalDateTime.now();
    }

}