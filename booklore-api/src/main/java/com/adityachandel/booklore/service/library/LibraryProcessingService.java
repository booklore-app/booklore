package com.adityachandel.booklore.service.library;

import com.adityachandel.booklore.exception.ApiError;
import com.adityachandel.booklore.model.dto.settings.LibraryFile;
import com.adityachandel.booklore.model.dto.Book;
import com.adityachandel.booklore.model.entity.BookEntity;
import com.adityachandel.booklore.model.entity.LibraryEntity;
import com.adityachandel.booklore.model.entity.LibraryPathEntity;
import com.adityachandel.booklore.model.enums.BookFileType;
import com.adityachandel.booklore.model.websocket.Topic;
import com.adityachandel.booklore.repository.BookRepository;
import com.adityachandel.booklore.repository.LibraryRepository;
import com.adityachandel.booklore.service.NotificationService;
import com.adityachandel.booklore.service.fileprocessor.CbxProcessor;
import com.adityachandel.booklore.service.fileprocessor.EpubProcessor;
import com.adityachandel.booklore.service.fileprocessor.PdfProcessor;
import com.adityachandel.booklore.util.FileUtils;
import jakarta.persistence.EntityManager;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static com.adityachandel.booklore.model.websocket.LogNotification.createLogNotification;

@Service
@AllArgsConstructor
@Slf4j
public class LibraryProcessingService {

    private final LibraryRepository libraryRepository;
    private final NotificationService notificationService;
    private final PdfProcessor pdfProcessor;
    private final EpubProcessor epubProcessor;
    private final CbxProcessor cbxProcessor;
    private final BookRepository bookRepository;
    private final EntityManager entityManager;
    private final TransactionTemplate transactionTemplate;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final Map<String, ScheduledFuture<?>> deletionTasks = new ConcurrentHashMap<>();
    private static final int DELETE_DEBOUNCE_SECONDS = 3;

    @Transactional
    public void processLibrary(long libraryId) throws IOException {
        LibraryEntity libraryEntity = libraryRepository.findById(libraryId).orElseThrow(() -> ApiError.LIBRARY_NOT_FOUND.createException(libraryId));
        notificationService.sendMessage(Topic.LOG, createLogNotification("Started processing library: " + libraryEntity.getName()));
        List<LibraryFile> libraryFiles = getLibraryFiles(libraryEntity);
        processLibraryFiles(libraryFiles);
        notificationService.sendMessage(Topic.LOG, createLogNotification("Finished processing library: " + libraryEntity.getName()));
    }

    @Transactional
    public void processFile(WatchEvent.Kind<?> eventKind, long libraryId, String libraryPath, String filePath) {
        LibraryEntity libraryEntity = libraryRepository.findById(libraryId).orElseThrow(() -> ApiError.LIBRARY_NOT_FOUND.createException(libraryId));
        Path path = Paths.get(filePath);
        String fileName = path.getFileName().toString();
        if (eventKind == StandardWatchEventKinds.ENTRY_CREATE) {
            ScheduledFuture<?> scheduledDeletion = deletionTasks.remove(filePath);
            if (scheduledDeletion != null) {
                scheduledDeletion.cancel(false);
                log.debug("Cancelled scheduled deletion for file: {}", filePath);
            }

            notificationService.sendMessage(Topic.LOG, createLogNotification("Started processing file: " + filePath));

            LibraryPathEntity libraryPathEntity = getLibraryPathEntityForFile(libraryEntity, libraryPath);
            libraryPathEntity = entityManager.merge(libraryPathEntity);

            LibraryPathEntity finalLibraryPathEntity = libraryPathEntity;
            bookRepository.findBookByFileNameAndLibraryId(fileName, libraryId)
                    .ifPresent(existingBook -> {
                        Path newFullPath = path;
                        Path oldFullPath = existingBook.getFullFilePath();
                        if (!newFullPath.equals(oldFullPath)) {
                            existingBook.setLibraryPath(finalLibraryPathEntity);
                            existingBook.setFileSubPath(FileUtils.getRelativeSubPath(finalLibraryPathEntity.getPath(), newFullPath));
                            bookRepository.save(existingBook);
                            log.debug("Updated path for moved book: {}", fileName);
                        }
                    });

            LibraryFile libraryFile = LibraryFile.builder()
                    .libraryEntity(libraryEntity)
                    .libraryPathEntity(libraryPathEntity)
                    .fileSubPath(FileUtils.getRelativeSubPath(libraryPathEntity.getPath(), path))
                    .fileName(fileName)
                    .bookFileType(getBookFileType(fileName))
                    .build();

            processLibraryFiles(List.of(libraryFile));
            notificationService.sendMessage(Topic.LOG, createLogNotification("Finished processing file: " + filePath));

        } else if (eventKind == StandardWatchEventKinds.ENTRY_DELETE) {
            ScheduledFuture<?> scheduledDeletion = deletionTasks.putIfAbsent(filePath, scheduler.schedule(() -> {
                try {
                    boolean fileStillExists = libraryEntity.getLibraryPaths().stream()
                            .anyMatch(lp -> {
                                Path fullPath = Path.of(lp.getPath()).resolve(fileName);
                                return Files.exists(fullPath);
                            });
                    if (!fileStillExists) {
                        transactionTemplate.executeWithoutResult(status -> {
                            bookRepository.findBookByFileNameAndLibraryId(fileName, libraryId)
                                    .ifPresent(bookEntity -> {
                                        deleteRemovedBooks(List.of(bookEntity.getId()));
                                        notificationService.sendMessage(Topic.BOOKS_REMOVE, Set.of(bookEntity.getId()));
                                        log.debug("Deleted book after debounce: {}", fileName);
                                    });
                        });
                    } else {
                        log.debug("File '{}' detected to still exist after debounce, skipping delete.", fileName);
                    }
                } catch (Exception e) {
                    log.error("Error during delayed deletion of file: {}", filePath, e);
                } finally {
                    deletionTasks.remove(filePath);
                }
            }, DELETE_DEBOUNCE_SECONDS, TimeUnit.SECONDS));

            // If scheduledDeletion != null, it means a task was already scheduled, so we skip scheduling again
        }
    }

    @Transactional
    protected LibraryPathEntity getLibraryPathEntityForFile(LibraryEntity libraryEntity, String libraryPath) {
        return libraryEntity.getLibraryPaths().stream().filter(l -> l.getPath().equals(libraryPath))
                .findFirst()
                .orElseThrow(() -> ApiError.BOOK_NOT_FOUND.createException(libraryPath));
    }

    @Transactional
    protected BookFileType getBookFileType(String fileName) {
        String lowerCaseName = fileName.toLowerCase();
        if (lowerCaseName.endsWith(".pdf")) {
            return BookFileType.PDF;
        } else if (lowerCaseName.endsWith(".cbz")) {
            return BookFileType.CBX;
        } else if (lowerCaseName.endsWith(".cbr")) {
            return BookFileType.CBX;
        } else if (lowerCaseName.endsWith(".cb7")) {
            return BookFileType.CBX;
        } else {
            return BookFileType.EPUB;
        }
    }

    @Transactional
    public void rescanLibrary(long libraryId) throws IOException {
        LibraryEntity libraryEntity = libraryRepository.findById(libraryId).orElseThrow(() -> ApiError.LIBRARY_NOT_FOUND.createException(libraryId));
        notificationService.sendMessage(Topic.LOG, createLogNotification("Started refreshing library: " + libraryEntity.getName()));
        detectMovedBooksAndUpdatePaths(libraryEntity);
        entityManager.flush();
        entityManager.clear();
        deleteRemovedBooks(detectDeletedBookIds(libraryEntity));
        entityManager.flush();
        entityManager.clear();
        processLibraryFiles(detectNewBookPaths(libraryEntity));
        entityManager.flush();
        entityManager.clear();
        notificationService.sendMessage(Topic.LOG, createLogNotification("Finished refreshing library: " + libraryEntity.getName()));
    }

    void detectMovedBooksAndUpdatePaths(LibraryEntity libraryEntity) throws IOException {
        List<LibraryFile> currentLibraryFiles = getLibraryFiles(libraryEntity);
        Map<String, Path> currentFileMap = currentLibraryFiles.stream()
                .collect(Collectors.toMap(LibraryFile::getFileName, LibraryFile::getFullPath, (existing, replacement) -> existing));
        for (BookEntity book : libraryEntity.getBookEntities()) {
            Path existingPath = book.getFullFilePath();
            Path updatedPath = currentFileMap.get(book.getFileName());
            if (updatedPath != null && !updatedPath.equals(existingPath)) {
                log.info("Detected moved book: '{}' from '{}' to '{}'", book.getFileName(), existingPath, updatedPath);
                libraryEntity.getLibraryPaths().stream()
                        .filter(lp -> updatedPath.startsWith(Path.of(lp.getPath())))
                        .findFirst()
                        .ifPresent(matchingPath -> {
                            book.setLibraryPath(matchingPath);
                            book.setFileSubPath(FileUtils.getRelativeSubPath(matchingPath.getPath(), updatedPath));
                        });
            }
        }
    }

    public List<Long> detectDeletedBookIds(LibraryEntity libraryEntity) throws IOException {
        Set<String> currentFileNames = getLibraryFiles(libraryEntity).stream()
                .map(LibraryFile::getFileName)
                .collect(Collectors.toSet());
        return libraryEntity.getBookEntities().stream()
                .filter(book -> !currentFileNames.contains(book.getFileName()))
                .map(BookEntity::getId)
                .collect(Collectors.toList());
    }

    public List<LibraryFile> detectNewBookPaths(LibraryEntity libraryEntity) throws IOException {
        Set<String> existingFileNames = libraryEntity.getBookEntities().stream()
                .map(BookEntity::getFileName)
                .collect(Collectors.toSet());
        return getLibraryFiles(libraryEntity).stream()
                .filter(file -> !existingFileNames.contains(file.getFileName()))
                .collect(Collectors.toList());
    }

    @Transactional
    protected void deleteRemovedBooks(List<Long> bookIds) {
        bookRepository.deleteByIdIn(bookIds);
        notificationService.sendMessage(Topic.BOOKS_REMOVE, bookIds);
        if (bookIds.size() > 1) {
            log.info("Books removed: {}", bookIds);
        }
    }

    @Transactional
    protected void processLibraryFiles(List<LibraryFile> libraryFiles) {
        for (LibraryFile libraryFile : libraryFiles) {
            log.info("Processing file: {}", libraryFile.getFileName());
            Book book = processLibraryFile(libraryFile);
            if (book != null) {
                notificationService.sendMessage(Topic.BOOK_ADD, book);
                notificationService.sendMessage(Topic.LOG, createLogNotification("Book added: " + book.getFileName()));
                log.info("Processed file: {}", libraryFile.getFileName());
            }
        }
    }

    @Transactional
    protected Book processLibraryFile(LibraryFile libraryFile) {
        if (libraryFile.getBookFileType() == BookFileType.PDF) {
            return pdfProcessor.processFile(libraryFile, false);
        } else if (libraryFile.getBookFileType() == BookFileType.EPUB) {
            return epubProcessor.processFile(libraryFile, false);
        } else if (libraryFile.getBookFileType() == BookFileType.CBX) {
            return cbxProcessor.processFile(libraryFile, false);
        }
        return null;
    }

    private List<LibraryFile> getLibraryFiles(LibraryEntity libraryEntity) throws IOException {
        List<LibraryFile> libraryFiles = new ArrayList<>();
        for (LibraryPathEntity libraryPathEntity : libraryEntity.getLibraryPaths()) {
            libraryFiles.addAll(findLibraryFiles(libraryPathEntity, libraryEntity));
        }
        return libraryFiles;
    }

    private List<LibraryFile> findLibraryFiles(LibraryPathEntity libraryPathEntity, LibraryEntity libraryEntity) throws IOException {
        List<LibraryFile> libraryFiles = new ArrayList<>();
        Path libraryPath = Path.of(libraryPathEntity.getPath());
        try (var stream = Files.walk(libraryPath)) {
            stream.filter(Files::isRegularFile)
                    .filter(file -> {
                        String fileName = file.getFileName().toString().toLowerCase();
                        return fileName.endsWith(".pdf") ||
                                fileName.endsWith(".epub") ||
                                fileName.endsWith(".cbz") ||
                                fileName.endsWith(".cbr") ||
                                fileName.endsWith(".cb7");
                    })
                    .forEach(fullFilePath -> {
                        String fileName = fullFilePath.getFileName().toString().toLowerCase();
                        BookFileType fileType;
                        if (fileName.endsWith(".pdf")) {
                            fileType = BookFileType.PDF;
                        } else if (fileName.endsWith(".epub")) {
                            fileType = BookFileType.EPUB;
                        } else if (fileName.endsWith(".cbz")) {
                            fileType = BookFileType.CBX;
                        } else if (fileName.endsWith(".cbr")) {
                            fileType = BookFileType.CBX;
                        } else if (fileName.endsWith(".cb7")) {
                            fileType = BookFileType.CBX;
                        } else {
                            return;
                        }

                        String relativePath = FileUtils.getRelativeSubPath(libraryPathEntity.getPath(), fullFilePath);
                        LibraryFile libraryFile = LibraryFile.builder()
                                .libraryEntity(libraryEntity)
                                .libraryPathEntity(libraryPathEntity)
                                .fileSubPath(relativePath)
                                .fileName(fullFilePath.getFileName().toString())
                                .bookFileType(fileType)
                                .build();
                        libraryFiles.add(libraryFile);
                    });
        }
        return libraryFiles;
    }
}