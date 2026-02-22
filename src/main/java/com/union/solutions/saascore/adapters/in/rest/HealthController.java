package com.union.solutions.saascore.adapters.in.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/healthz")
    public ResponseEntity<String> healthz() {
        return ResponseEntity.ok("OK");
    }

    @GetMapping("/readyz")
    public ResponseEntity<String> readyz() {
        return ResponseEntity.ok("OK");
    }
}
