package com.wherewear.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Lightweight authenticated ping the frontend polls right after sign-in to
 * detect Render's free-tier cold start (the service sleeps after ~15 min
 * idle and can take up to a minute to wake) - see AppShell in the frontend.
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }
}
