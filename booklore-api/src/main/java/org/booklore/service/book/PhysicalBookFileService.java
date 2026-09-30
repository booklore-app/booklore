package org.booklore.service.book;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.booklore.config.AppProperties;
import org.booklore.exception.ApiError;
import org.booklore.model.entity.AuthorEntity;
import org.booklore.model.entity.BookEntity;
import org.booklore.model.entity.BookFileEntity;
import org.booklore.model.entity.BookMetadataEntity;
import org.booklore.model.entity.CategoryEntity;
import org.booklore.model.entity.LibraryEntity;
import org.booklore.model.entity.LibraryPathEntity;
import org.booklore.model.enums.BookFileType;
import org.booklore.repository.BookAdditionalFileRepository;
import org.booklore.service.file.FileFingerprint;
import org.booklore.service.metadata.sidecar.SidecarMetadataWriter;
import org.booklore.service.monitoring.MonitoringRegistrationService;
import org.booklore.util.FileService;
import org.booklore.util.PathPatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Gives a physical book a real file in its library folder, so it behaves like every other book
 * (it has a path on disk, follows the library's naming pattern, can be moved by the file organizer,
 * and notes and sidecar files can live next to it) while it stays flagged as a physical book.
 * <p>
 * The file is a small placeholder EPUB (see {@link PlaceholderEpubBuilder}). Its file record is marked with
 * {@link #PLACEHOLDER_DESCRIPTION} so it can be recognised, and replaced when a real digital file is uploaded.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PhysicalBookFileService {

    public static final String PLACEHOLDER_DESCRIPTION = "Physical book placeholder";

    private static final int MAX_NAME_ATTEMPTS = 1000;

    /**
     * Physical books are always filed as Author/Title/Title.epub, whatever naming pattern the library uses for
     * its digital books. Books without an author go under "Unknown Author".
     */
    static final String FILE_PATTERN = "<{authors}|Unknown Author>/{title}/{title}.{extension}";

    private final AppProperties appProperties;
    private final MonitoringRegistrationService monitoringRegistrationService;
    private final FileService fileService;
    private final BookAdditionalFileRepository bookFileRepository;
    private final SidecarMetadataWriter sidecarMetadataWriter;

    public static boolean isPlaceholder(BookFileEntity file) {
        return file != null && PLACEHOLDER_DESCRIPTION.equals(file.getDescription());
    }

    /**
     * True for the placeholder file of a book that is flagged as physical. Such a file exists only so the book has
     * a place on disk; it is not shown as a readable format and is never synced, read or re-scanned as content.
     */
    public static boolean isHiddenPlaceholder(BookFileEntity file) {
        return isPlaceholder(file)
                && file.getBook() != null
                && Boolean.TRUE.equals(file.getBook().getIsPhysical());
    }

    /**
     * Creates the placeholder file for a physical book that has no file yet, and attaches the file record
     * and library path to the book. The caller is responsible for saving the book.
     *
     * @return true if a file was created; false if nothing was done (book already has files, the library
     * is not on local storage, or the library has no folder configured)
     * @throws org.booklore.exception.APIException if the file could not be written
     */
    public boolean attachPlaceholderFile(BookEntity book) {
        if (book.hasFiles()) {
            return false;
        }
        if (!appProperties.isLocalStorage()) {
            log.debug("Not creating a file for physical book {}: library is not on local storage", book.getId());
            return false;
        }

        LibraryEntity library = book.getLibrary();
        BookMetadataEntity metadata = book.getMetadata();
        if (library == null || metadata == null || library.getLibraryPaths() == null || library.getLibraryPaths().isEmpty()) {
            log.warn("Not creating a file for physical book {}: it has no library folder or metadata", book.getId());
            return false;
        }

        LibraryPathEntity libraryPath = book.getLibraryPath() != null ? book.getLibraryPath() : library.getLibraryPaths().getFirst();
        Path root = Paths.get(libraryPath.getPath()).toAbsolutePath().normalize();
        Path target = resolveFreeTarget(root, library, metadata);

        byte[] epub;
        try {
            epub = PlaceholderEpubBuilder.build(specFrom(metadata, readCover(book.getId())));
        } catch (IOException e) {
            throw ApiError.INTERNAL_SERVER_ERROR.createException("Could not build the file for the physical book: " + e.getMessage());
        }

        Long libraryId = library.getId();
        List<Path> libraryRoots = library.getLibraryPaths().stream().map(p -> Path.of(p.getPath())).toList();
        boolean pauseMonitoring = libraryId != null && monitoringRegistrationService.isLibraryMonitored(libraryId);
        if (pauseMonitoring) {
            monitoringRegistrationService.unregisterLibrary(libraryId);
        }

        boolean written = false;
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, epub, StandardOpenOption.CREATE_NEW);
            written = true;

            String hash = FileFingerprint.generateHash(target);
            Path relativeParent = root.relativize(target.getParent());

            BookFileEntity fileEntity = BookFileEntity.builder()
                    .book(book)
                    .fileName(target.getFileName().toString())
                    .fileSubPath(relativeParent.toString())
                    .isBookFormat(true)
                    .bookType(BookFileType.EPUB)
                    .fileSizeKb(epub.length / 1024L)
                    .initialHash(hash)
                    .currentHash(hash)
                    .description(PLACEHOLDER_DESCRIPTION)
                    .addedOn(Instant.now())
                    .build();

            book.setLibraryPath(libraryPath);
            BookFileEntity saved = bookFileRepository.save(fileEntity);
            if (book.getBookFiles() == null) {
                book.setBookFiles(new ArrayList<>());
            }
            book.getBookFiles().add(saved);

            deleteFileIfTransactionFails(target);
            writeSidecarQuietly(book);

            log.info("Created placeholder file for physical book {}: {}", book.getId(), target);
            return true;
        } catch (IOException e) {
            if (written) {
                deleteQuietly(target);
            }
            throw ApiError.INTERNAL_SERVER_ERROR.createException("Could not create the file for the physical book (is the library folder writable?): " + e.getMessage());
        } catch (RuntimeException e) {
            if (written) {
                deleteQuietly(target);
            }
            throw e;
        } finally {
            if (pauseMonitoring) {
                resumeMonitoring(libraryId, libraryRoots);
            }
        }
    }

    private Path resolveFreeTarget(Path root, LibraryEntity library, BookMetadataEntity metadata) {
        String baseName = safeFileName(metadata.getTitle());
        String relative = PathPatternResolver.resolvePattern(metadata, FILE_PATTERN, baseName + ".epub");

        Path first = root.resolve(relative).normalize();
        if (!first.startsWith(root) || first.equals(root)) {
            throw ApiError.INTERNAL_SERVER_ERROR.createException("The library naming pattern produced a path outside the library folder: " + relative);
        }
        if (!first.getFileName().toString().toLowerCase().endsWith(".epub")) {
            first = first.resolveSibling(first.getFileName() + ".epub");
        }

        Path candidate = first;
        String name = first.getFileName().toString();
        int dot = name.lastIndexOf('.');
        String stem = dot > 0 ? name.substring(0, dot) : name;
        String extension = dot > 0 ? name.substring(dot) : "";
        for (int i = 2; Files.exists(candidate) && i <= MAX_NAME_ATTEMPTS; i++) {
            candidate = first.resolveSibling(stem + " (" + i + ")" + extension);
        }
        if (Files.exists(candidate)) {
            throw ApiError.INTERNAL_SERVER_ERROR.createException("Could not find a free file name for the physical book: " + first);
        }
        return candidate;
    }

    private static String safeFileName(String title) {
        String cleaned = title == null ? "" : title.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", " ").replaceAll("\\s+", " ").trim();
        if (cleaned.length() > 150) {
            cleaned = cleaned.substring(0, 150).trim();
        }
        return cleaned.isEmpty() ? "Untitled" : cleaned;
    }

    private PlaceholderEpubBuilder.Spec specFrom(BookMetadataEntity metadata, byte[] cover) {
        List<String> authors = metadata.getAuthors() == null ? List.of()
                : metadata.getAuthors().stream().map(AuthorEntity::getName).filter(Objects::nonNull).toList();
        List<String> subjects = metadata.getCategories() == null ? List.of()
                : metadata.getCategories().stream().map(CategoryEntity::getName).filter(Objects::nonNull).toList();
        String isbn = metadata.getIsbn13() != null ? metadata.getIsbn13() : metadata.getIsbn10();
        String published = metadata.getPublishedDate() != null ? metadata.getPublishedDate().toString() : null;
        return new PlaceholderEpubBuilder.Spec(metadata.getTitle(), authors, metadata.getDescription(), metadata.getPublisher(),
                published, metadata.getLanguage(), isbn, metadata.getPageCount(), subjects, cover);
    }

    private byte[] readCover(Long bookId) {
        if (bookId == null) {
            return null;
        }
        try {
            Path coverPath = Path.of(fileService.getCoverFile(bookId));
            return Files.isRegularFile(coverPath) ? Files.readAllBytes(coverPath) : null;
        } catch (Exception e) {
            log.debug("No cover to embed for physical book {}: {}", bookId, e.getMessage());
            return null;
        }
    }

    /** If the surrounding transaction rolls back, the file must not be left behind for the scanner to import. */
    private void deleteFileIfTransactionFails(Path file) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    deleteQuietly(file);
                }
            }
        });
    }

    /**
     * Monitoring is paused while the file is written so the folder watcher does not import it as a new book,
     * and resumed once the transaction has finished (so the file is already known to the database).
     */
    private void resumeMonitoring(Long libraryId, List<Path> libraryRoots) {
        Runnable resume = () -> {
            try {
                for (Path libraryRoot : libraryRoots) {
                    monitoringRegistrationService.registerLibraryPaths(libraryId, libraryRoot);
                }
            } catch (Exception e) {
                log.warn("Failed to resume monitoring for library {}: {}", libraryId, e.getMessage());
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    resume.run();
                }
            });
        } else {
            resume.run();
        }
    }

    private void writeSidecarQuietly(BookEntity book) {
        try {
            if (sidecarMetadataWriter.isWriteOnScanEnabled()) {
                sidecarMetadataWriter.writeSidecarMetadata(book);
            }
        } catch (Exception e) {
            log.warn("Could not write sidecar metadata for physical book {}: {}", book.getId(), e.getMessage());
        }
    }

    private void deleteQuietly(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            log.warn("Could not remove placeholder file {}: {}", file, e.getMessage());
        }
    }
}
