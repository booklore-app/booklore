package com.adityachandel.booklore.service.metadata.writer;

import com.adityachandel.booklore.model.entity.AuthorEntity;
import com.adityachandel.booklore.model.entity.BookMetadataEntity;
import com.adityachandel.booklore.model.entity.CategoryEntity;

import java.time.LocalDate;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class MetadataCopyHelper {

    private final BookMetadataEntity metadata;

    public MetadataCopyHelper(BookMetadataEntity metadata) {
        this.metadata = metadata;
    }

    private boolean isLocked(Boolean lockedFlag) {
        return Boolean.TRUE.equals(lockedFlag);
    }

    public void copyTitle(boolean restoreMode, Consumer<String> consumer) {
        if (!isLocked(metadata.getTitleLocked())) {
            if (metadata.getTitle() != null || restoreMode) {
                consumer.accept(metadata.getTitle());
            }
        }
    }

    public void copySubtitle(boolean restoreMode, Consumer<String> consumer) {
        if (!isLocked(metadata.getSubtitleLocked())) {
            if (metadata.getSubtitle() != null || restoreMode) {
                consumer.accept(metadata.getSubtitle());
            }
        }
    }

    public void copyPublisher(boolean restoreMode, Consumer<String> consumer) {
        if (!isLocked(metadata.getPublisherLocked())) {
            if (metadata.getPublisher() != null || restoreMode) {
                consumer.accept(metadata.getPublisher());
            }
        }
    }

    public void copyPublishedDate(boolean restoreMode, Consumer<LocalDate> consumer) {
        if (!isLocked(metadata.getPublishedDateLocked())) {
            if (metadata.getPublishedDate() != null || restoreMode) {
                consumer.accept(metadata.getPublishedDate());
            }
        }
    }

    public void copyDescription(boolean restoreMode, Consumer<String> consumer) {
        if (!isLocked(metadata.getDescriptionLocked())) {
            if (metadata.getDescription() != null || restoreMode) {
                consumer.accept(metadata.getDescription());
            }
        }
    }

    public void copySeriesName(boolean restoreMode, Consumer<String> consumer) {
        if (!isLocked(metadata.getSeriesNameLocked())) {
            if (metadata.getSeriesName() != null || restoreMode) {
                consumer.accept(metadata.getSeriesName());
            }
        }
    }

    public void copySeriesNumber(boolean restoreMode, Consumer<Float> consumer) {
        if (!isLocked(metadata.getSeriesNumberLocked())) {
            if (metadata.getSeriesNumber() != null || restoreMode) {
                consumer.accept(metadata.getSeriesNumber());
            }
        }
    }

    public void copySeriesTotal(boolean restoreMode, Consumer<Integer> consumer) {
        if (!isLocked(metadata.getSeriesTotalLocked())) {
            if (metadata.getSeriesTotal() != null || restoreMode) {
                consumer.accept(metadata.getSeriesTotal());
            }
        }
    }

    public void copyIsbn13(boolean restoreMode, Consumer<String> consumer) {
        if (!isLocked(metadata.getIsbn13Locked())) {
            if (metadata.getIsbn13() != null || restoreMode) {
                consumer.accept(metadata.getIsbn13());
            }
        }
    }

    public void copyIsbn10(boolean restoreMode, Consumer<String> consumer) {
        if (!isLocked(metadata.getIsbn10Locked())) {
            if (metadata.getIsbn10() != null || restoreMode) {
                consumer.accept(metadata.getIsbn10());
            }
        }
    }

    public void copyAsin(boolean restoreMode, Consumer<String> consumer) {
        if (!isLocked(metadata.getAsinLocked())) {
            if (metadata.getAsin() != null || restoreMode) {
                consumer.accept(metadata.getAsin());
            }
        }
    }

    public void copyPageCount(boolean restoreMode, Consumer<Integer> consumer) {
        if (!isLocked(metadata.getPageCountLocked())) {
            if (metadata.getPageCount() != null || restoreMode) {
                consumer.accept(metadata.getPageCount());
            }
        }
    }

    public void copyLanguage(boolean restoreMode, Consumer<String> consumer) {
        if (!isLocked(metadata.getLanguageLocked())) {
            if (metadata.getLanguage() != null || restoreMode) {
                consumer.accept(metadata.getLanguage());
            }
        }
    }

    public void copyAuthors(Consumer<Set<String>> consumer) {
        if (!isLocked(metadata.getAuthorsLocked()) && metadata.getAuthors() != null) {
            Set<String> names = metadata.getAuthors().stream()
                    .map(AuthorEntity::getName)
                    .filter(n -> n != null && !n.isBlank())
                    .collect(Collectors.toSet());
            if (!names.isEmpty()) consumer.accept(names);
        }
    }

    public void copyCategories(Consumer<Set<String>> consumer) {
        if (!isLocked(metadata.getCategoriesLocked()) && metadata.getCategories() != null) {
            Set<String> cats = metadata.getCategories().stream()
                    .map(CategoryEntity::getName)
                    .filter(n -> n != null && !n.isBlank())
                    .collect(Collectors.toSet());
            consumer.accept(cats);
        }
    }

    public void copyGoodreadsId(boolean restoreMode, Consumer<String> consumer) {
        if (!isLocked(metadata.getGoodreadsIdLocked())) {
            if (metadata.getGoodreadsId() != null || restoreMode) {
                consumer.accept(metadata.getGoodreadsId());
            }
        }
    }

    public void copyHardcoverId(boolean restoreMode, Consumer<String> consumer) {
        if (!isLocked(metadata.getHardcoverIdLocked())) {
            if (metadata.getHardcoverId() != null || restoreMode) {
                consumer.accept(metadata.getHardcoverId());
            }
        }
    }

    public void copyGoogleId(boolean restoreMode, Consumer<String> consumer) {
        if (!isLocked(metadata.getGoogleIdLocked())) {
            if (metadata.getGoogleId() != null || restoreMode) {
                consumer.accept(metadata.getGoogleId());
            }
        }
    }

    public void copyPersonalRating(boolean restoreMode, Consumer<Double> consumer) {
        if (!isLocked(metadata.getPersonalRatingLocked())) {
            if (metadata.getPersonalRating() != null || restoreMode) {
                consumer.accept(metadata.getPersonalRating());
            }
        }
    }
}
