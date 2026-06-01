package com.bookstore.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Exposes the application's build version (from {@code pom.xml}, injected at build
 * time via Maven resource filtering of {@code application.yml}). Public endpoint so
 * the SPA footer can display it for anonymous visitors too.
 */
@RestController
@RequestMapping("/api/version")
public class VersionController {

    private final String version;

    public VersionController(@Value("${app.version:dev}") String version) {
        this.version = version;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getVersion() {
        Map<String, Object> body = new HashMap<>();
        body.put("version", version);
        return ResponseEntity.ok(body);
    }

}
