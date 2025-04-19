package com.adityachandel.booklore.service;

import com.adityachandel.booklore.model.dto.ReleaseNote;
import com.adityachandel.booklore.model.dto.VersionInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class VersionService {

    @Value("${app.version:unknown}")
    private String appVersion;

    private static final String GITHUB_REPO = "adityachandelgit/BookLore";

    public VersionInfo getVersionInfo() {
        String latestVersion = fetchLatestGitHubReleaseVersion();
        return new VersionInfo(appVersion, latestVersion);
    }

    public List<ReleaseNote> getChangelogSinceCurrentVersion() {
        return fetchReleaseNotesSince(appVersion);
    }

    private String fetchLatestGitHubReleaseVersion() {
        try {
            RestClient restClient = RestClient.builder()
                    .defaultHeader("Accept", "application/vnd.github+json")
                    .defaultHeader("User-Agent", "BookLore-Version-Checker")
                    .build();

            String response = restClient.get()
                    .uri("https://api.github.com/repos/" + GITHUB_REPO + "/releases/latest")
                    .retrieve()
                    .body(String.class);

            JsonNode root = new ObjectMapper().readTree(response);
            return root.has("tag_name") ? root.get("tag_name").asText() : "unknown";

        } catch (Exception e) {
            log.error("Failed to fetch latest release version", e);
            return "unknown";
        }
    }

    private List<ReleaseNote> fetchReleaseNotesSince(String currentVersion) {
        List<ReleaseNote> updates = new ArrayList<>();
        try {
            RestClient restClient = RestClient.builder()
                    .defaultHeader("Accept", "application/vnd.github+json")
                    .defaultHeader("User-Agent", "BookLore-Version-Checker")
                    .build();

            String response = restClient.get()
                    .uri("https://api.github.com/repos/" + GITHUB_REPO + "/releases")
                    .retrieve()
                    .body(String.class);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode releases = mapper.readTree(response);

            for (JsonNode release : releases) {
                String tag = release.get("tag_name").asText();
                if (isVersionGreater(tag, currentVersion)) {
                    String url = "https://github.com/" + GITHUB_REPO + "/releases/tag/" + tag;
                    updates.add(new ReleaseNote(
                            tag,
                            release.get("name").asText(),
                            release.get("body").asText(),
                            url
                    ));
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch release notes", e);
        }
        return updates;
    }

    private boolean isVersionGreater(String version1, String version2) {
        try {
            String[] v1 = version1.replace("v", "").split("\\.");
            String[] v2 = version2.replace("v", "").split("\\.");
            for (int i = 0; i < Math.max(v1.length, v2.length); i++) {
                int num1 = i < v1.length ? Integer.parseInt(v1[i]) : 0;
                int num2 = i < v2.length ? Integer.parseInt(v2[i]) : 0;
                if (num1 > num2) return true;
                if (num1 < num2) return false;
            }
        } catch (Exception e) {
            log.error("Version comparison failed", e);
        }
        return false;
    }
}