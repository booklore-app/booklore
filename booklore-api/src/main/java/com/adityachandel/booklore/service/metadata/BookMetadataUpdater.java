package com.adityachandel.booklore.service.metadata;

import com.adityachandel.booklore.exception.ApiError;
import com.adityachandel.booklore.model.dto.BookMetadata;
import com.adityachandel.booklore.model.dto.settings.MetadataPersistenceSettings;
import com.adityachandel.booklore.model.entity.AuthorEntity;
import com.adityachandel.booklore.model.entity.BookEntity;
import com.adityachandel.booklore.model.entity.BookMetadataEntity;
import com.adityachandel.booklore.model.entity.CategoryEntity;
import com.adityachandel.booklore.model.enums.BookFileType;
import com.adityachandel.booklore.repository.AuthorRepository;
import com.adityachandel.booklore.repository.BookMetadataRepository;
import com.adityachandel.booklore.repository.BookRepository;
import com.adityachandel.booklore.repository.CategoryRepository;
import com.adityachandel.booklore.service.appsettings.AppSettingService;
import com.adityachandel.booklore.util.FileService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class BookMetadataUpdater {

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final BookMetadataRepository bookMetadataRepository;
    private final CategoryRepository categoryRepository;
    private final FileService fileService;
    private final MetadataMatchService metadataMatchService;
    private final AppSettingService appSettingService;
    private final MetadataBackupRestoreService metadataBackupRestoreService;
    private final EpubMetadataWriter epubMetadataWriter;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BookMetadataEntity setBookMetadata(BookEntity bookEntity, BookMetadata newMetadata, boolean setThumbnail, boolean mergeCategories) {
        BookMetadataEntity metadata = bookEntity.getMetadata();

        updateLocks(newMetadata, metadata);

        if (metadata.areAllFieldsLocked()) {
            log.warn("Attempted to update metadata for book with ID {}, but all fields are locked. No update performed.", bookEntity.getId());
            return metadata;
        }

        MetadataPersistenceSettings settings = appSettingService.getAppSettings().getMetadataPersistenceSettings();
        boolean writeToFile = settings.isSaveToOriginalFile();
        boolean backupEnabled = settings.isBackupMetadata();
        boolean backupCover = settings.isBackupCover();
        BookFileType bookType = bookEntity.getBookType();

        if (writeToFile && backupEnabled && bookType == BookFileType.EPUB) {
            try {
                metadataBackupRestoreService.backupEmbeddedMetadataIfNotExists(bookEntity, backupCover);
            } catch (Exception e) {
                log.warn("Failed to backup metadata for book ID {}: {}", bookEntity.getId(), e.getMessage());
            }
        }

        updateBasicFields(newMetadata, metadata);
        updateAuthorsIfNeeded(newMetadata, metadata);
        updateCategoriesIfNeeded(newMetadata, metadata, mergeCategories);
        updateThumbnailIfNeeded(bookEntity.getId(), newMetadata, metadata, setThumbnail);

        bookMetadataRepository.save(metadata);

        try {
            Float score = metadataMatchService.calculateMatchScore(bookEntity);
            bookEntity.setMetadataMatchScore(score);
            bookRepository.save(bookEntity);
        } catch (Exception e) {
            log.warn("Failed to calculate/save metadata match score for book ID {}: {}", bookEntity.getId(), e.getMessage());
        }

        if (writeToFile && bookType == BookFileType.EPUB) {
            try {
                File bookFile = new File(bookEntity.getFullFilePath().toUri());
                epubMetadataWriter.writeMetadataToFile(bookFile, metadata);
                log.info("Embedded metadata written to EPUB for book ID {}", bookEntity.getId());
            } catch (Exception e) {
                log.warn("Failed to write metadata to EPUB for book ID {}: {}", bookEntity.getId(), e.getMessage());
            }
        }

        return metadata;
    }

    private void updateBasicFields(BookMetadata newMetadata, BookMetadataEntity metadata) {
        updateFieldIfUnlocked(metadata::getGoodreadsIdLocked, newMetadata.getGoodreadsId(), v -> metadata.setGoodreadsId(nullIfBlank(v)));
        updateFieldIfUnlocked(metadata::getHardcoverIdLocked, newMetadata.getHardcoverId(), v -> metadata.setHardcoverId(nullIfBlank(v)));
        updateFieldIfUnlocked(metadata::getGoogleIdLocked, newMetadata.getGoogleId(), v -> metadata.setGoogleId(nullIfBlank(v)));
        updateFieldIfUnlocked(metadata::getTitleLocked, newMetadata.getTitle(), v -> metadata.setTitle(nullIfBlank(v)));
        updateFieldIfUnlocked(metadata::getSubtitleLocked, newMetadata.getSubtitle(), v -> metadata.setSubtitle(nullIfBlank(v)));
        updateFieldIfUnlocked(metadata::getPublisherLocked, newMetadata.getPublisher(), v -> metadata.setPublisher(nullIfBlank(v)));
        updateFieldIfUnlocked(metadata::getPublishedDateLocked, newMetadata.getPublishedDate(), metadata::setPublishedDate);
        updateFieldIfUnlocked(metadata::getLanguageLocked, newMetadata.getLanguage(), v -> metadata.setLanguage(nullIfBlank(v)));
        updateFieldIfUnlocked(metadata::getIsbn10Locked, newMetadata.getIsbn10(), v -> metadata.setIsbn10(nullIfBlank(v)));
        updateFieldIfUnlocked(metadata::getIsbn13Locked, newMetadata.getIsbn13(), v -> metadata.setIsbn13(nullIfBlank(v)));
        updateFieldIfUnlocked(metadata::getAsinLocked, newMetadata.getAsin(), v -> metadata.setAsin(nullIfBlank(v)));
        updateFieldIfUnlocked(metadata::getDescriptionLocked, newMetadata.getDescription(), v -> metadata.setDescription(nullIfBlank(v)));
        updateFieldIfUnlocked(metadata::getPageCountLocked, newMetadata.getPageCount(), metadata::setPageCount);
        updateFieldIfUnlocked(metadata::getAmazonRatingLocked, newMetadata.getAmazonRating(), metadata::setAmazonRating);
        updateFieldIfUnlocked(metadata::getAmazonReviewCountLocked, newMetadata.getAmazonReviewCount(), metadata::setAmazonReviewCount);
        updateFieldIfUnlocked(metadata::getGoodreadsRatingLocked, newMetadata.getGoodreadsRating(), metadata::setGoodreadsRating);
        updateFieldIfUnlocked(metadata::getGoodreadsReviewCountLocked, newMetadata.getGoodreadsReviewCount(), metadata::setGoodreadsReviewCount);
        updateFieldIfUnlocked(metadata::getHardcoverRatingLocked, newMetadata.getHardcoverRating(), metadata::setHardcoverRating);
        updateFieldIfUnlocked(metadata::getHardcoverReviewCountLocked, newMetadata.getHardcoverReviewCount(), metadata::setHardcoverReviewCount);
        updateFieldIfUnlocked(metadata::getSeriesNameLocked, newMetadata.getSeriesName(), metadata::setSeriesName);
        updateFieldIfUnlocked(metadata::getSeriesNumberLocked, newMetadata.getSeriesNumber(), metadata::setSeriesNumber);
        updateFieldIfUnlocked(metadata::getSeriesTotalLocked, newMetadata.getSeriesTotal(), metadata::setSeriesTotal);
    }

    private void updateAuthorsIfNeeded(BookMetadata newMetadata, BookMetadataEntity metadata) {
        if (shouldUpdateField(metadata.getAuthorsLocked(), newMetadata.getAuthors()) && newMetadata.getAuthors() != null && !newMetadata.getAuthors().isEmpty()) {
            metadata.setAuthors(newMetadata.getAuthors().stream()
                    .filter(a -> a != null && !a.isBlank())
                    .map(authorName -> authorRepository.findByName(authorName)
                            .orElseGet(() -> authorRepository.save(AuthorEntity.builder().name(authorName).build())))
                    .collect(Collectors.toSet()));
        }
    }

    private void updateCategoriesIfNeeded(BookMetadata newMetadata, BookMetadataEntity metadata, boolean mergeCategories) {
        if (shouldUpdateField(metadata.getCategoriesLocked(), newMetadata.getCategories()) && newMetadata.getCategories() != null) {
            if (mergeCategories) {
                HashSet<CategoryEntity> existingCategories = new HashSet<>(metadata.getCategories());
                newMetadata.getCategories().stream()
                        .filter(c -> c != null && !c.isBlank())
                        .forEach(categoryName -> {
                            CategoryEntity categoryEntity = categoryRepository.findByName(categoryName)
                                    .orElseGet(() -> categoryRepository.save(CategoryEntity.builder().name(categoryName).build()));
                            existingCategories.add(categoryEntity);
                        });
                metadata.setCategories(new HashSet<>(existingCategories));
            } else if (!newMetadata.getCategories().isEmpty()) {
                metadata.setCategories(newMetadata.getCategories().stream()
                        .filter(c -> c != null && !c.isBlank())
                        .map(categoryName -> categoryRepository.findByName(categoryName)
                                .orElseGet(() -> categoryRepository.save(CategoryEntity.builder().name(categoryName).build())))
                        .collect(Collectors.toSet()));
            }
        }
    }

    private void updateThumbnailIfNeeded(long bookId, BookMetadata newMetadata, BookMetadataEntity metadata, boolean setThumbnail) {
        if (setThumbnail && shouldUpdateField(metadata.getThumbnailLocked(), newMetadata.getThumbnailUrl()) && !newMetadata.getThumbnailUrl().isEmpty()) {
            String thumbnailPath = null;
            try {
                thumbnailPath = fileService.createThumbnail(bookId, newMetadata.getThumbnailUrl());
                metadata.setCoverUpdatedOn(Instant.now());
            } catch (IOException e) {
                log.error(e.getMessage());
            }
            metadata.setThumbnail(thumbnailPath);
        }
    }

    private boolean shouldUpdateField(Boolean locked, Object newValue) {
        return (locked == null || !locked) && newValue != null;
    }

    private String nullIfBlank(String value) {
        return StringUtils.hasText(value) ? value : null;
    }

    private <T> void updateFieldIfUnlocked(Supplier<Boolean> lockSupplier, T newValue, Consumer<T> setter) {
        if (shouldUpdateField(lockSupplier.get(), newValue)) {
            setter.accept(newValue);
        }
    }

    private void updateLocks(BookMetadata newMetadata, BookMetadataEntity metadata) {
        List<Pair<Boolean, Consumer<Boolean>>> lockMappings = List.of(
                Pair.of(newMetadata.getTitleLocked(), metadata::setTitleLocked),
                Pair.of(newMetadata.getSubtitleLocked(), metadata::setSubtitleLocked),
                Pair.of(newMetadata.getPublisherLocked(), metadata::setPublisherLocked),
                Pair.of(newMetadata.getPublishedDateLocked(), metadata::setPublishedDateLocked),
                Pair.of(newMetadata.getDescriptionLocked(), metadata::setDescriptionLocked),
                Pair.of(newMetadata.getIsbn13Locked(), metadata::setIsbn13Locked),
                Pair.of(newMetadata.getIsbn10Locked(), metadata::setIsbn10Locked),
                Pair.of(newMetadata.getAsinLocked(), metadata::setAsinLocked),
                Pair.of(newMetadata.getGoodreadsIdLocked(), metadata::setGoodreadsIdLocked),
                Pair.of(newMetadata.getHardcoverIdLocked(), metadata::setHardcoverIdLocked),
                Pair.of(newMetadata.getGoogleIdLocked(), metadata::setGoogleIdLocked),
                Pair.of(newMetadata.getPageCountLocked(), metadata::setPageCountLocked),
                Pair.of(newMetadata.getLanguageLocked(), metadata::setLanguageLocked),
                Pair.of(newMetadata.getAmazonRatingLocked(), metadata::setAmazonRatingLocked),
                Pair.of(newMetadata.getAmazonReviewCountLocked(), metadata::setAmazonReviewCountLocked),
                Pair.of(newMetadata.getGoodreadsRatingLocked(), metadata::setGoodreadsRatingLocked),
                Pair.of(newMetadata.getGoodreadsReviewCountLocked(), metadata::setGoodreadsReviewCountLocked),
                Pair.of(newMetadata.getHardcoverRatingLocked(), metadata::setHardcoverRatingLocked),
                Pair.of(newMetadata.getHardcoverReviewCountLocked(), metadata::setHardcoverReviewCountLocked),
                Pair.of(newMetadata.getSeriesNameLocked(), metadata::setSeriesNameLocked),
                Pair.of(newMetadata.getSeriesNumberLocked(), metadata::setSeriesNumberLocked),
                Pair.of(newMetadata.getSeriesTotalLocked(), metadata::setSeriesTotalLocked),
                Pair.of(newMetadata.getAuthorsLocked(), metadata::setAuthorsLocked),
                Pair.of(newMetadata.getCategoriesLocked(), metadata::setCategoriesLocked),
                Pair.of(newMetadata.getCoverLocked(), metadata::setCoverLocked)
        );
        lockMappings.forEach(pair -> applyLock(pair.getLeft(), pair.getRight()));
    }

    private void applyLock(Boolean newLockValue, Consumer<Boolean> setter) {
        if (newLockValue != null) {
            setter.accept(newLockValue);
        }
    }

}