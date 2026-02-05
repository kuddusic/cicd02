package com.example.helloworld.web;

import java.time.Instant;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @Value("${HELLO_MESSAGE:Hello OpenShift}")
    private String message;

    @GetMapping("/")
    public Map<String, Object> hello() {
        return Map.of(
            "message", message,
            "timestamp", Instant.now().toString()
        );
    }
}
