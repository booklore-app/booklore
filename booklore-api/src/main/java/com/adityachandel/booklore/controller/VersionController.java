package com.adityachandel.booklore.controller;

import com.adityachandel.booklore.model.dto.ReleaseNote;
import com.adityachandel.booklore.model.dto.VersionInfo;
import com.adityachandel.booklore.service.VersionService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@AllArgsConstructor
@RestController
@RequestMapping("/api/v1/version")
public class VersionController {

    private final VersionService versionService;

    @GetMapping
    public ResponseEntity<VersionInfo> getVersionInfo() {
        return ResponseEntity.ok(versionService.getVersionInfo());
    }

    @GetMapping("/changelog")
    public ResponseEntity<List<ReleaseNote>> getChangelogSinceCurrent() {
        return ResponseEntity.ok(versionService.getChangelogSinceCurrentVersion());
    }
}