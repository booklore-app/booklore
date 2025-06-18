package com.adityachandel.booklore.service.metadata;

import com.adityachandel.booklore.exception.ApiError;
import com.adityachandel.booklore.model.dto.EpubMetadata;
import com.adityachandel.booklore.model.entity.BookEntity;
import com.adityachandel.booklore.model.entity.BookMetadataEntity;
import com.adityachandel.booklore.repository.BookRepository;
import com.adityachandel.booklore.util.FileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import io.documentnode.epub4j.domain.Book;
import io.documentnode.epub4j.domain.Resource;
import io.documentnode.epub4j.epub.EpubReader;
import io.documentnode.epub4j.epub.EpubWriter;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

@Slf4j
@Service
@AllArgsConstructor
public class MetadataBackupRestoreService {

    private final FileService fileService;
    private final ObjectMapper objectMapper;
    private final BookRepository bookRepository;
    private final EpubMetadataReader epubMetadataReader;
    private final BookMetadataRestorer bookMetadataRestorer;

    public void backupEmbeddedMetadataIfNotExists(BookEntity bookEntity, boolean backupCover) {
        File bookFile = new File(bookEntity.getFullFilePath().toUri());
        Path backupDir = resolveBackupDir(bookEntity);
        Path metadataFile = backupDir.resolve("metadata.json");
        Path coverFile = backupDir.resolve("cover.jpg");
        Path filenameCheckFile = backupDir.resolve("original-filename.txt");

        try {
            if (Files.exists(metadataFile)) {
                return;
            }

            Files.createDirectories(backupDir);

            EpubMetadata metadata = epubMetadataReader.readMetadata(bookFile);
            String json = objectMapper.writer().writeValueAsString(metadata);
            Files.writeString(metadataFile, json, StandardOpenOption.CREATE_NEW);

            Files.writeString(filenameCheckFile, bookEntity.getFileName(), StandardOpenOption.CREATE_NEW);

            if (backupCover) {
                try (FileInputStream fis = new FileInputStream(bookFile)) {
                    Book epubBook = new EpubReader().readEpub(fis);
                    Resource coverImage = epubBook.getCoverImage();
                    if (coverImage != null) {
                        Files.write(coverFile, coverImage.getData(), StandardOpenOption.CREATE_NEW);
                        log.info("Backup cover image saved for book ID {} at {}", bookEntity.getId(), coverFile);
                    } else {
                        log.warn("No cover image found in EPUB for book ID {}", bookEntity.getId());
                    }
                }
            }

            log.info("Created backup of embedded metadata for book ID {} at {}", bookEntity.getId(), backupDir);

        } catch (Exception e) {
            log.warn("Failed to create metadata backup for book ID {}", bookEntity.getId(), e);
        }
    }

    public void restoreEmbeddedMetadata(BookEntity bookEntity) throws IOException {
        File bookFile = new File(bookEntity.getFullFilePath().toUri());
        Path backupDir = resolveBackupDir(bookEntity);
        Path metadataFile = backupDir.resolve("metadata.json");
        Path coverFile = backupDir.resolve("cover.jpg");
        Path filenameCheckFile = backupDir.resolve("original-filename.txt");

        if (Files.notExists(metadataFile)) {
            throw ApiError.INTERNAL_SERVER_ERROR.createException("Metadata backup file not found.");
        }

        if (Files.notExists(filenameCheckFile)) {
            throw ApiError.INTERNAL_SERVER_ERROR.createException("Filename check file is missing.");
        }

        String backedUpFilename = Files.readString(filenameCheckFile).trim();
        String currentFilename = bookEntity.getFileName().trim();

        if (!currentFilename.equals(backedUpFilename)) {
            throw ApiError.INTERNAL_SERVER_ERROR.createException("The backup is for a different file.");
        }

        EpubMetadata backupMetadata = readEpubMetadata(metadataFile, bookEntity.getId());
        bookMetadataRestorer.restoreMetadata(bookEntity, backupMetadata);

        if (Files.exists(coverFile)) {
            try {
                byte[] coverData = Files.readAllBytes(coverFile);
                Resource cover = new Resource(coverData, "images/cover.jpg");
                cover.setId("cover-image");

                Book epubBook = new EpubReader().readEpub(new FileInputStream(bookFile));
                epubBook.getResources().remove("images/cover.jpg");
                epubBook.getResources().add(cover);
                epubBook.setCoverImage(cover);

                File tempFile = new File(bookFile.getParentFile(), bookFile.getName() + ".tmp");
                try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                    new EpubWriter().write(epubBook, fos);
                }

                if (!bookFile.delete()) {
                    throw new IOException("Could not delete original EPUB file during restore.");
                }
                if (!tempFile.renameTo(bookFile)) {
                    throw new IOException("Could not rename temp EPUB file during restore.");
                }

                log.info("Restored cover image from backup for book ID {}", bookEntity.getId());
            } catch (Exception e) {
                log.warn("Failed to restore cover image for book ID {}: {}", bookEntity.getId(), e.getMessage(), e);
            }
        }

        updateThumbnailIfNeeded(bookEntity.getId(), coverFile, bookEntity.getMetadata());

        log.info("Successfully restored embedded metadata for book ID {}", bookEntity.getId());
    }

    private void updateThumbnailIfNeeded(long bookId, Path coverPath, BookMetadataEntity metadata) {
        String thumbnailPath = null;
        try {
            thumbnailPath = fileService.createThumbnail(bookId, coverPath.toString());
            metadata.setCoverUpdatedOn(Instant.now());
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        metadata.setThumbnail(thumbnailPath);
    }

    public EpubMetadata getBackedUpMetadata(Long bookId) {
        BookEntity bookEntity = bookRepository.findById(bookId).orElseThrow(() -> ApiError.BOOK_NOT_FOUND.createException(bookId));

        Path metadataFile = resolveBackupDir(bookEntity).resolve("metadata.json");

        if (Files.notExists(metadataFile)) {
            throw ApiError.INTERNAL_SERVER_ERROR.createException("Metadata backup file not found.");
        }

        try {
            ObjectReader reader = objectMapper.readerFor(EpubMetadata.class);
            return reader.readValue(metadataFile.toFile());
        } catch (IOException e) {
            log.error("Failed to read metadata backup for book ID {}: {}", bookId, e.getMessage(), e);
            throw ApiError.INTERNAL_SERVER_ERROR.createException("Failed to read metadata backup file.");
        }
    }

    private EpubMetadata readEpubMetadata(Path metadataFile, Long bookId) {
        try {
            ObjectReader reader = objectMapper.readerFor(EpubMetadata.class);
            return reader.readValue(metadataFile.toFile());
        } catch (IOException e) {
            log.error("Failed to read metadata backup for book ID {}: {}", bookId, e.getMessage(), e);
            throw ApiError.INTERNAL_SERVER_ERROR.createException("Failed to read metadata backup file.");
        }
    }

    private Path resolveBackupDir(BookEntity bookEntity) {
        return Path.of(fileService.getMetadataBackupPath(), String.valueOf(bookEntity.getId()));
    }

    public org.springframework.core.io.Resource getBackupCover(long bookId) {
        BookEntity bookEntity = bookRepository.findById(bookId)
                .orElseThrow(() -> ApiError.BOOK_NOT_FOUND.createException(bookId));

        Path coverPath = resolveBackupDir(bookEntity).resolve("cover.jpg");

        if (Files.notExists(coverPath)) {
            log.warn("No cover image found in backup for book ID {} at {}", bookId, coverPath);
            throw ApiError.INTERNAL_SERVER_ERROR.createException("Backup cover image not found.");
        }

        return new FileSystemResource(coverPath);
    }
}