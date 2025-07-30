package com.adityachandel.booklore.service.scheduler;

import com.adityachandel.booklore.repository.MetadataFetchJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetadataFetchCleanupService {

    private final MetadataFetchJobRepository metadataFetchJobRepository;

    @Scheduled(cron = "0 30 0 * * *") // Runs every day at 00:30 AM
    public void cleanupOldMetadataFetchJobs() {
        Instant cutoff = Instant.now().minus(5, ChronoUnit.DAYS);
        int deleted = metadataFetchJobRepository.deleteAllByCompletedAtBefore(cutoff);
        log.info("MetadataFetchCleanupService: Removed {} metadata fetch jobs older than {}", deleted, cutoff);
    }
}
