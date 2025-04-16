package com.adityachandel.booklore.controller;

import com.adityachandel.booklore.model.dto.VersionInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@RestController
@RequestMapping("/api/v1/version")
public class VersionController {

    @Value("${app.version:unknown}")
    private String appVersion;

    @GetMapping
    public ResponseEntity<VersionInfo> getVersions() {
        String latestVersion = fetchLatestGitHubReleaseVersion();
        return ResponseEntity.ok(new VersionInfo(appVersion, latestVersion));
    }

    private String fetchLatestGitHubReleaseVersion() {
        try {
            RestClient restClient = RestClient.builder()
                    .defaultHeader("Accept", "application/vnd.github+json")
                    .defaultHeader("User-Agent", "BookLore-Version-Checker")
                    .build();

            String response = restClient.get()
                    .uri("https://api.github.com/repos/adityachandelgit/BookLore/releases/latest")
                    .retrieve()
                    .body(String.class);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode root = objectMapper.readTree(response);
            return root.has("tag_name") ? root.get("tag_name").asText() : "unknown";

        } catch (Exception e) {
            return "unknown";
        }
    }
}
