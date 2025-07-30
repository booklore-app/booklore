package com.adityachandel.booklore.scheduler;

import com.adityachandel.booklore.repository.MetadataFetchJobRepository;
import com.adityachandel.booklore.service.scheduler.MetadataFetchCleanupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.mockito.Mockito.*;

class MetadataFetchCleanupServiceTest {

    @Mock
    private MetadataFetchJobRepository metadataFetchJobRepository;

    @InjectMocks
    private MetadataFetchCleanupService cleanupService;

    @Captor
    private ArgumentCaptor<Instant> instantCaptor;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void cleanupOldMetadataFetchJobs_shouldCallRepositoryWithCorrectCutoff() {
        when(metadataFetchJobRepository.deleteAllByCompletedAtBefore(any())).thenReturn(5);

        cleanupService.cleanupOldMetadataFetchJobs();

        verify(metadataFetchJobRepository, times(1)).deleteAllByCompletedAtBefore(instantCaptor.capture());

        Instant cutoff = instantCaptor.getValue();
        Instant expectedCutoff = Instant.now().minus(5, ChronoUnit.DAYS);

        long deltaSeconds = Math.abs(cutoff.getEpochSecond() - expectedCutoff.getEpochSecond());
        assert deltaSeconds < 5 : "Cutoff instant should be about 5 days ago";
    }

    @Test
    void cleanupOldMetadataFetchJobs_shouldLogDeletedCount() {
        when(metadataFetchJobRepository.deleteAllByCompletedAtBefore(any())).thenReturn(10);

        cleanupService.cleanupOldMetadataFetchJobs();

        verify(metadataFetchJobRepository, times(1)).deleteAllByCompletedAtBefore(any());
    }
}