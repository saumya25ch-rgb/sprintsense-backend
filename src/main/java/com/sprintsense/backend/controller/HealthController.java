package com.sprintsense.backend.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
public class HealthController {

    @GetMapping("/api/health")
    public String health() {
        return "SprintSense Backend is up and running!";
    }
}
