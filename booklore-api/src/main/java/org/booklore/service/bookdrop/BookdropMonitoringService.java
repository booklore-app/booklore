package org.booklore.service.bookdrop;

import org.booklore.config.AppProperties;
import org.booklore.model.enums.BookFileExtension;
import org.booklore.repository.BookdropFileRepository;
import org.booklore.util.FileUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@Slf4j
@Service
public class BookdropMonitoringService {

    private final AppProperties appProperties;
    private final BookdropEventHandlerService eventHandler;
    private final BookdropFileRepository bookdropFileRepository;

    private Path bookdrop;
    private WatchService watchService;
    private Thread watchThread;
    private volatile boolean running;
    private final ConcurrentHashMap<Path, WatchKey> registeredKeys = new ConcurrentHashMap<>();
    private volatile boolean paused;
    private volatile boolean disabled;

    public BookdropMonitoringService(
            AppProperties appProperties,
            BookdropEventHandlerService eventHandler,
            BookdropFileRepository bookdropFileRepository
    ) {
        this.appProperties = appProperties;
        this.eventHandler = eventHandler;
        this.bookdropFileRepository = bookdropFileRepository;
    }

    @PostConstruct
    public void start() {
        bookdrop = Path.of(appProperties.getBookdropFolder());
        if (Files.notExists(bookdrop)) {
            try {
                Files.createDirectories(bookdrop);
                log.info("Created missing bookdrop folder: {}", bookdrop);
            } catch (IOException e) {
                log.warn("Bookdrop folder is not available at '{}'. Bookdrop monitoring is disabled. " +
                        "Mount a volume at this path to enable it.", bookdrop);
                this.disabled = true;
                return;
            }
        }

        try {
            log.info("Starting bookdrop folder monitor: {}", bookdrop);
            this.watchService = FileSystems.getDefault().newWatchService();
            registerDirectoryRecursive(bookdrop);
            this.running = true;
            this.paused = false;
            this.watchThread = new Thread(this::processEvents, "BookdropFolderWatcher");
            this.watchThread.setDaemon(true);
            this.watchThread.start();
            scanExistingBookdropFiles();
        } catch (IOException e) {
            log.warn("Failed to start bookdrop folder monitor. Bookdrop monitoring is disabled.", e);
            this.disabled = true;
        }
    }

    @PreDestroy
    public void stop() {
        running = false;
        if (watchThread != null) {
            watchThread.interrupt();
        }
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException e) {
                log.error("Error closing WatchService", e);
            }
        }
        log.info("Stopped bookdrop folder monitor");
    }

    public synchronized void pauseMonitoring() {
        if (disabled) return;
        if (!paused) {
            registeredKeys.values().forEach(WatchKey::cancel);
            registeredKeys.clear();
            paused = true;
            log.info("Bookdrop monitoring paused.");
        } else {
            log.info("Bookdrop monitoring already paused.");
        }
    }

    public synchronized void resumeMonitoring() {
        if (disabled) return;
        if (paused) {
            registerDirectoryRecursive(bookdrop);
            paused = false;
            log.info("Bookdrop monitoring resumed.");
        } else {
            log.info("Bookdrop monitoring is not paused, cannot resume.");
        }
    }

    private void processEvents() {
        while (running) {
            if (paused) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    log.info("Bookdrop monitor thread interrupted during pause");
                    Thread.currentThread().interrupt();
                    return;
                }
                continue;
            }

            WatchKey key;
            try {
                key = watchService.take();
            } catch (InterruptedException e) {
                log.info("Bookdrop monitor thread interrupted");
                Thread.currentThread().interrupt();
                return;
            } catch (ClosedWatchServiceException e) {
                log.info("WatchService closed, stopping thread");
                return;
            }

            Path watchedDir = (Path) key.watchable();

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();

                if (kind == StandardWatchEventKinds.OVERFLOW) {
                    log.warn("Overflow event detected");
                    continue;
                }

                Path context = (Path) event.context();
                Path fullPath = watchedDir.resolve(context);

                log.info("Detected {} event on: {}", kind.name(), fullPath);

                if (kind == StandardWatchEventKinds.ENTRY_CREATE || kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                    if (Files.isDirectory(fullPath)) {
                        log.info("New directory detected, registering and scanning recursively: {}", fullPath);
                        registerDirectoryRecursive(fullPath);
                        try (Stream<Path> pathStream = Files.walk(fullPath)) {
                            pathStream
                                    .filter(Files::isRegularFile)
                                    .filter(path -> !FileUtils.shouldIgnore(path))
                                    .filter(path -> BookFileExtension.fromFileName(path.getFileName().toString()).isPresent())
                                    .forEach(path -> eventHandler.enqueueFile(path, StandardWatchEventKinds.ENTRY_CREATE));
                        } catch (IOException e) {
                            log.error("Failed to scan new directory: {}", fullPath, e);
                        }
                    } else {
                        if (!FileUtils.shouldIgnore(fullPath)) {
                            if (BookFileExtension.fromFileName(fullPath.getFileName().toString()).isPresent()) {
                                eventHandler.enqueueFile(fullPath, kind);
                            } else {
                                log.info("Ignored unsupported file type: {}", fullPath);
                            }
                        }
                    }
                } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                    if (Files.isDirectory(fullPath)) {
                        log.info("Directory deleted: {}, performing bulk DB cleanup", fullPath);
                        WatchKey deletedKey = registeredKeys.remove(fullPath);
                        if (deletedKey != null) {
                            deletedKey.cancel();
                        }
                    } else {
                        log.info("File deleted: {}", fullPath);
                    }
                    eventHandler.enqueueFile(fullPath, kind);
                }
            }

            boolean valid = key.reset();
            if (!valid) {
                log.warn("WatchKey no longer valid for: {}", watchedDir);
                registeredKeys.remove(watchedDir);
            }
        }
    }

    private void registerDirectoryRecursive(Path root) {
        try (Stream<Path> stream = Files.walk(root)) {
            stream.filter(Files::isDirectory).forEach(dir -> {
                if (!registeredKeys.containsKey(dir)) {
                    try {
                        WatchKey key = dir.register(watchService,
                                StandardWatchEventKinds.ENTRY_CREATE,
                                StandardWatchEventKinds.ENTRY_DELETE);
                        registeredKeys.put(dir, key);
                        log.debug("Registered bookdrop directory: {}", dir);
                    } catch (IOException e) {
                        log.warn("Failed to register bookdrop directory {}: {}", dir, e.getMessage());
                    }
                }
            });
        } catch (IOException e) {
            log.warn("Failed to walk bookdrop directory tree {}: {}", root, e.getMessage());
        }
    }

    public void rescanBookdropFolder() {
        if (disabled) {
            log.warn("Bookdrop monitoring is disabled. Skipping rescan.");
            return;
        }
        log.info("Rescan of Bookdrop folder triggered.");
        scanExistingBookdropFiles();
    }

    private void scanExistingBookdropFiles() {
        List<Path> supportedFiles;
        try (Stream<Path> files = Files.walk(bookdrop)) {
            supportedFiles = files.filter(Files::isRegularFile)
                    .filter(path -> !FileUtils.shouldIgnore(path))
                    .filter(path -> BookFileExtension.fromFileName(path.getFileName().toString()).isPresent())
                    .toList();
        } catch (IOException e) {
            log.error("Error scanning bookdrop folder", e);
            return;
        }

        if (!supportedFiles.isEmpty()) {
            List<String> supportedFilePaths = supportedFiles.stream()
                    .map(Path::toAbsolutePath)
                    .map(Path::toString)
                    .toList();
            List<String> knownFilePaths = bookdropFileRepository.findAllFilePathsIn(supportedFilePaths);
            Set<String> knownPaths = knownFilePaths == null ? Set.of() : new HashSet<>(knownFilePaths);

            supportedFilePaths.stream()
                    .filter(path -> !knownPaths.contains(path))
                    .map(Path::of)
                    .forEach(path -> {
                        log.info("Found existing supported file: {}", path);
                        eventHandler.enqueueFile(path, StandardWatchEventKinds.ENTRY_CREATE);
                    });
        }
    }
}
