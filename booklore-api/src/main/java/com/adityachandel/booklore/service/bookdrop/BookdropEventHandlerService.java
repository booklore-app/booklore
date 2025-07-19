package com.adityachandel.booklore.service.bookdrop;

import com.adityachandel.booklore.model.BookDropFileEvent;
import com.adityachandel.booklore.model.entity.BookdropFileEntity;
import com.adityachandel.booklore.model.enums.BookFileExtension;
import com.adityachandel.booklore.model.websocket.LogNotification;
import com.adityachandel.booklore.model.websocket.Topic;
import com.adityachandel.booklore.repository.BookdropFileRepository;
import com.adityachandel.booklore.service.NotificationService;
import com.adityachandel.booklore.service.appsettings.AppSettingService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.time.Instant;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookdropEventHandlerService {

    private final BookdropFileRepository bookdropFileRepository;
    private final NotificationService notificationService;
    private final BookdropNotificationService bookdropNotificationService;
    private final AppSettingService appSettingService;
    private final BookdropMetadataService bookdropMetadataService;

    private final BlockingQueue<BookDropFileEvent> fileQueue = new LinkedBlockingQueue<>();
    private volatile boolean running = true;
    private Thread workerThread;

    @PostConstruct
    public void init() {
        workerThread = new Thread(this::processQueue, "BookdropFileProcessor");
        workerThread.start();
    }

    @PreDestroy
    public void shutdown() {
        running = false;
        if (workerThread != null) {
            workerThread.interrupt();
        }
    }

    public void enqueueFile(Path file, WatchEvent.Kind<?> kind) {
        BookDropFileEvent event = new BookDropFileEvent(file, kind);
        if (!fileQueue.contains(event)) {
            fileQueue.offer(event);
        }
    }

    private void processQueue() {
        while (running) {
            try {
                processFile(fileQueue.take());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.info("File processing thread interrupted, shutting down.");
            }
        }
    }

    public void processFile(BookDropFileEvent event) {
        Path file = event.getFile();
        WatchEvent.Kind<?> kind = event.getKind();
        if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
            try {
                if (!Files.exists(file)) {
                    log.warn("File does not exist, ignoring: {}", file);
                    return;
                }

                if (Files.isDirectory(file)) {
                    log.info("New folder detected in bookdrop, ignoring: {}", file);
                    return;
                }

                String filePath = file.toAbsolutePath().toString();
                String fileName = file.getFileName().toString();

                if (BookFileExtension.fromFileName(fileName).isEmpty()) {
                    log.info("Unsupported file type detected, ignoring file: {}", fileName);
                    return;
                }

                if (bookdropFileRepository.findByFilePath(filePath).isPresent()) {
                    log.info("File already processed in bookdrop, ignoring: {}", filePath);
                    return;
                }

                log.info("Handling new bookdrop file: {}", file);

                int queueSize = fileQueue.size();
                notificationService.sendMessage(Topic.LOG, new LogNotification("Processing bookdrop file: " + fileName + " (" + queueSize + " books remaining)"));

                BookdropFileEntity bookdropFileEntity = BookdropFileEntity.builder()
                        .filePath(filePath)
                        .fileName(fileName)
                        .fileSize(Files.size(file))
                        .status(BookdropFileEntity.Status.PENDING_REVIEW)
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build();

                bookdropFileEntity = bookdropFileRepository.save(bookdropFileEntity);

                if (appSettingService.getAppSettings().isMetadataDownloadOnBookdrop()) {
                    bookdropMetadataService.attachInitialMetadata(bookdropFileEntity.getId());
                    bookdropMetadataService.attachFetchedMetadata(bookdropFileEntity.getId());
                } else {
                    bookdropMetadataService.attachInitialMetadata(bookdropFileEntity.getId());
                    log.info("Metadata download is disabled in settings. Only initial metadata extracted for file: {}", bookdropFileEntity.getFileName());
                }
                bookdropNotificationService.sendBookdropFileSummaryNotification();

                notificationService.sendMessage(Topic.LOG, new LogNotification("Finished processing bookdrop file: " + fileName + " (" + queueSize + " books remaining)"));
            } catch (Exception e) {
                log.error("Error handling bookdrop file: {}", file, e);
            }
        } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
            String deletedPath = event.getFile().toAbsolutePath().toString();
            log.info("Detected deletion event: {}", deletedPath);

            int deletedCount = bookdropFileRepository.deleteAllByFilePathStartingWith(deletedPath);
            log.info("Deleted {} BookdropFile record(s) from database matching path: {}", deletedCount, deletedPath);
            bookdropNotificationService.sendBookdropFileSummaryNotification();
        }
    }
}