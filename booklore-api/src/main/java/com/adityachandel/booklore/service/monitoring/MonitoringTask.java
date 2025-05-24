package com.adityachandel.booklore.service.monitoring;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.file.*;

@Slf4j
@Service
@AllArgsConstructor
public class MonitoringTask {

    private final WatchService watchService;
    private final ApplicationEventPublisher eventPublisher;

    private static final String PDF_EXTENSION = ".pdf";
    private static final String EPUB_EXTENSION = ".epub";

    @Async
    public void monitor() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                WatchKey key;
                try {
                    key = watchService.take();
                } catch (ClosedWatchServiceException e) {
                    log.warn("WatchService has been closed. Stopping monitoring.");
                    break;
                }
                if (key == null) {
                    continue;
                }
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    Path fileName = (Path) event.context();
                    Path directory = (Path) key.watchable();
                    Path fullPath = directory.resolve(fileName);

                    if (isPdfOrEpub(fileName) && (kind == StandardWatchEventKinds.ENTRY_CREATE || kind == StandardWatchEventKinds.ENTRY_DELETE)) {
                        log.debug("Event kind: {}; File affected: {}; Full path: {}; Watched folder: {}", kind, fileName, fullPath, directory);
                        eventPublisher.publishEvent(new FileChangeEvent(this, fullPath, kind, directory));
                    }
                }
                boolean valid = key.reset();
                if (!valid) {
                    log.warn("WatchKey is no longer valid. Stopping monitoring for {}", key.watchable());
                    break;
                }
            }
        } catch (InterruptedException e) {
            log.warn("Monitoring task interrupted", e);
            Thread.currentThread().interrupt();
        }
    }

    private boolean isPdfOrEpub(Path fileName) {
        String fileNameStr = fileName.toString().toLowerCase();
        return fileNameStr.endsWith(PDF_EXTENSION) || fileNameStr.endsWith(EPUB_EXTENSION);
    }
}