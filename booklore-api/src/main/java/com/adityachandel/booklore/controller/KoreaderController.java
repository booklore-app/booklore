package com.adityachandel.booklore.controller;

import com.adityachandel.booklore.config.security.KoreaderUserDetails;
import com.adityachandel.booklore.model.dto.progress.KoreaderProgress;
import com.adityachandel.booklore.service.KoreaderService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/api/koreader")
public class KoreaderController {

    private final KoreaderService koreaderService;

    @GetMapping("/users/auth")
    public ResponseEntity<Map<String, String>> authorizeUser() {
        return koreaderService.authorizeUser();
    }

    @PostMapping("/users/create")
    public ResponseEntity<?> createUser(@RequestBody Map<String, Object> userData) {
        log.warn("Attempt to register user via Koreader blocked: {}", userData);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "User registration via Koreader is disabled"));
    }

    @GetMapping("/syncs/progress/{bookHash}")
    public ResponseEntity<KoreaderProgress> getProgress(@PathVariable String bookHash) {
        KoreaderProgress progress = koreaderService.getProgress(bookHash);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(progress);
    }

    @PutMapping("/syncs/progress")
    public ResponseEntity<?> updateProgress(@Valid @RequestBody KoreaderProgress koreaderProgress) {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof KoreaderUserDetails)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not authenticated or invalid principal type"));
        }
        Long userId = ((KoreaderUserDetails) principal).getBookLoreUserId();
        koreaderService.saveProgress(koreaderProgress.getDocument(), koreaderProgress, userId);
        return ResponseEntity.ok(Map.of("status", "progress updated"));
    }
}