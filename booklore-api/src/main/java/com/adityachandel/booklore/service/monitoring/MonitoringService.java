package com.adityachandel.booklore.service.monitoring;

import com.adityachandel.booklore.model.dto.Library;
import com.adityachandel.booklore.service.LibraryProcessingService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

@Slf4j
@Service
@AllArgsConstructor
public class MonitoringService {

    private final LibraryProcessingService libraryProcessingService;
    private final WatchService watchService;
    private final MonitoringTask monitoringTask;

    private final Set<Path> monitoredPaths = ConcurrentHashMap.newKeySet();
    private final Map<Path, Long> pathToLibraryIdMap = new ConcurrentHashMap<>();

    private final BlockingQueue<FileChangeEvent> eventQueue = new LinkedBlockingQueue<>();
    private final ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();

    @PostConstruct
    public void initializeMonitoring() {
        monitoringTask.monitor();
        startProcessingThread();
    }

    public void registerLibrariesForMonitoring(List<Library> libraries) {
        libraries.stream()
                .filter(Library::isWatch)
                .forEach(library -> {
                    library.getPaths().forEach(libraryPath -> {
                        Path path = Paths.get(libraryPath.getPath());
                        if (Files.isDirectory(path)) {
                            registerPath(path, library.getId());
                        }
                    });
                });
        log.info("Registered libraries for monitoring: {}", libraries.size());
    }

    public synchronized void registerPath(Path path, Long libraryId) {
        try {
            if (monitoredPaths.add(path)) {
                path.register(watchService,
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_MODIFY,
                        StandardWatchEventKinds.ENTRY_DELETE);
                pathToLibraryIdMap.put(path, libraryId);
                log.info("Registered folder for monitoring: {} (Library ID: {})", path, libraryId);
            } else {
                log.warn("Path is already registered: {}", path);
            }
        } catch (IOException e) {
            log.error("Error registering path: {}", path, e);
        }
    }

    public synchronized void unregisterPath(String folderPath) {
        Path path = Paths.get(folderPath);
        if (monitoredPaths.remove(path)) {
            pathToLibraryIdMap.remove(path);
            log.info("Unregistered folder from monitoring: {}", folderPath);
        } else {
            log.warn("Folder not found in monitored paths: {}", folderPath);
        }
    }

    @EventListener
    public void handleFileChangeEvent(FileChangeEvent event) {
        if (!eventQueue.offer(event)) {
            log.warn("Event queue is full, dropping event: {}", event.getFilePath());
        } else {
            log.debug("Queued file change event: {} ({} in queue)", event.getFilePath(), eventQueue.size());
        }
    }

    private void startProcessingThread() {
        singleThreadExecutor.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    FileChangeEvent event = eventQueue.take();
                    processFileChangeEvent(event);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }

    private void processFileChangeEvent(FileChangeEvent event) {
        Path filePath = event.getFilePath();
        Path watchedFolder = event.getWatchedFolder();
        Long libraryId = pathToLibraryIdMap.get(watchedFolder);

        if (libraryId != null) {
            try {
                libraryProcessingService.processFile(event.getEventKind(), libraryId, watchedFolder.toString(), filePath.toString());
            } catch (InvalidDataAccessApiUsageException e) {
                log.debug("InvalidDataAccessApiUsageException - Library id: {}", libraryId);
            }
            log.debug("Processed file change event for library {}: {} (from folder: {}) with kind: {}", libraryId, filePath, watchedFolder, event.getEventKind());
        } else {
            log.warn("No libraryId found for watched folder: {}", watchedFolder);
        }
    }

    @PreDestroy
    public void stopMonitoring() {
        log.info("Shutting down monitoring service...");
        singleThreadExecutor.shutdownNow();
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException e) {
                log.error("Exception while closing the WatchService", e);
            }
        }
    }
}