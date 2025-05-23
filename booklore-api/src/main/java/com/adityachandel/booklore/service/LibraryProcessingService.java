package com.adityachandel.booklore.service;

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
import com.adityachandel.booklore.service.fileprocessor.EpubProcessor;
import com.adityachandel.booklore.service.fileprocessor.PdfProcessor;
import com.adityachandel.booklore.util.FileUtils;
import jakarta.persistence.EntityManager;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
    private final BookRepository bookRepository;
    private final EntityManager entityManager;

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
        LibraryEntity libraryEntity = libraryRepository.findById(libraryId)
                .orElseThrow(() -> ApiError.LIBRARY_NOT_FOUND.createException(libraryId));

        Path path = Paths.get(filePath);
        String fileName = path.getFileName().toString();

        if (eventKind == StandardWatchEventKinds.ENTRY_CREATE) {
            notificationService.sendMessage(Topic.LOG, createLogNotification("Started processing file: " + filePath));

            LibraryPathEntity libraryPathEntity = getLibraryPathEntityForFile(libraryEntity, libraryPath);
            libraryPathEntity = entityManager.merge(libraryPathEntity);

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
            bookRepository.findBookByFileNameAndLibraryId(fileName, libraryId)
                .ifPresent(bookEntity -> {
                    deleteRemovedBooks(List.of(bookEntity));
                    notificationService.sendMessage(Topic.BOOKS_REMOVE, Set.of(bookEntity.getId()));
                });
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
        if (fileName.endsWith(".pdf")) {
            return BookFileType.PDF;
        } else {
            return BookFileType.EPUB;
        }
    }

    @Transactional
    public void rescanLibrary(long libraryId) throws IOException {
        LibraryEntity libraryEntity = libraryRepository.findById(libraryId).orElseThrow(() -> ApiError.LIBRARY_NOT_FOUND.createException(libraryId));
        notificationService.sendMessage(Topic.LOG, createLogNotification("Started refreshing library: " + libraryEntity.getName()));
        processLibraryFiles(getUnProcessedFiles(libraryEntity));
        deleteRemovedBooks(getRemovedBooks(libraryEntity));
        notificationService.sendMessage(Topic.LOG, createLogNotification("Finished refreshing library: " + libraryEntity.getName()));
    }

    @Transactional
    protected void deleteRemovedBooks(List<BookEntity> removedBookEntities) {
        if (!removedBookEntities.isEmpty()) {
            entityManager.flush();
            entityManager.clear();
            Set<Long> bookIds = removedBookEntities.stream().map(BookEntity::getId).collect(Collectors.toSet());
            bookRepository.deleteByIdIn(bookIds);
            notificationService.sendMessage(Topic.BOOKS_REMOVE, bookIds);
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
        }
        return null;
    }

    @Transactional
    protected List<BookEntity> getRemovedBooks(LibraryEntity libraryEntity) throws IOException {
        List<LibraryFile> libraryFiles = getLibraryFiles(libraryEntity);
        List<BookEntity> bookEntities = libraryEntity.getBookEntities();
        Set<String> libraryFilePaths = libraryFiles.stream()
                .map(LibraryFile::getFileName)
                .collect(Collectors.toSet());
        return bookEntities.stream()
                .filter(book -> !libraryFilePaths.contains(book.getFileName()))
                .collect(Collectors.toList());
    }

    @Transactional
    protected List<LibraryFile> getUnProcessedFiles(LibraryEntity libraryEntity) throws IOException {
        List<LibraryFile> libraryFiles = getLibraryFiles(libraryEntity);
        List<BookEntity> bookEntities = libraryEntity.getBookEntities();
        Set<String> processedPaths = bookEntities.stream()
                .map(BookEntity::getFileName)
                .collect(Collectors.toSet());
        return libraryFiles.stream()
                .filter(libraryFile -> !processedPaths.contains(libraryFile.getFileName()))
                .collect(Collectors.toList());
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
                        return fileName.endsWith(".pdf") || fileName.endsWith(".epub");
                    })
                    .forEach(fullFilePath -> {
                        BookFileType fileType = fullFilePath.getFileName().toString().toLowerCase().endsWith(".pdf")
                                ? BookFileType.PDF
                                : BookFileType.EPUB;
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