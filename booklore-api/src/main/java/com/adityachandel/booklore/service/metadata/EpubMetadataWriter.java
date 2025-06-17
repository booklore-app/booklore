package com.adityachandel.booklore.service.metadata;

import com.adityachandel.booklore.model.entity.AuthorEntity;
import com.adityachandel.booklore.model.entity.BookMetadataEntity;
import com.adityachandel.booklore.model.entity.CategoryEntity;
import io.documentnode.epub4j.domain.*;
import io.documentnode.epub4j.epub.EpubReader;
import io.documentnode.epub4j.epub.EpubWriter;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.io.*;
import java.util.*;
import java.util.Date;
import java.util.stream.Collectors;

@Slf4j
@Component
public class EpubMetadataWriter {

    private static final String OPF_NS = "http://www.idpf.org/2007/opf";

    @Transactional
    public void writeMetadataToFile(File epubFile, BookMetadataEntity metadata) {
        try {
            Book book = new EpubReader().readEpub(new FileInputStream(epubFile));
            Metadata meta = book.getMetadata();

            if (StringUtils.isNotBlank(metadata.getTitle())) {
                meta.getTitles().clear();
                meta.addTitle(metadata.getTitle());
            }

            Set<AuthorEntity> authors = metadata.getAuthors();
            if (authors != null && !authors.isEmpty()) {
                meta.getAuthors().clear();
                for (AuthorEntity author : authors) {
                    String[] parts = author.getName().trim().split(" ", 2);
                    String firstName = parts.length > 1 ? parts[0] : "";
                    String lastName = parts.length > 1 ? parts[1] : parts[0];
                    meta.addAuthor(new Author(firstName, lastName));
                }
            }

            if (StringUtils.isNotBlank(metadata.getDescription())) {
                meta.getDescriptions().clear();
                meta.addDescription(metadata.getDescription());
            }

            if (StringUtils.isNotBlank(metadata.getPublisher())) {
                meta.getPublishers().clear();
                meta.addPublisher(metadata.getPublisher());
            }

            if (StringUtils.isNotBlank(metadata.getLanguage())) {
                meta.setLanguage(metadata.getLanguage());
            }

            if (metadata.getPublishedDate() != null) {
                meta.getDates().clear();
                meta.addDate(new io.documentnode.epub4j.domain.Date(metadata.getPublishedDate().toString()));
            }

            meta.getIdentifiers().removeIf(id -> {
                String scheme = Optional.ofNullable(id.getScheme()).orElse("").toUpperCase();
                return scheme.equals("ISBN") || scheme.equals("ASIN");
            });

            if (StringUtils.isNotBlank(metadata.getIsbn13())) {
                meta.addIdentifier(new Identifier(Identifier.Scheme.ISBN, metadata.getIsbn13()));
            } else if (StringUtils.isNotBlank(metadata.getIsbn10())) {
                meta.addIdentifier(new Identifier(Identifier.Scheme.ISBN, metadata.getIsbn10()));
            } else if (StringUtils.isNotBlank(metadata.getAsin())) {
                meta.addIdentifier(new Identifier("ASIN", metadata.getAsin()));
            }

            var categories = metadata.getCategories();
            if (categories != null && !categories.isEmpty()) {
                meta.getSubjects().clear();
                meta.getSubjects().addAll(categories.stream().map(CategoryEntity::getName).distinct().toList());
            }

            Map<QName, String> otherProps = new HashMap<>();

            putIfValid(otherProps, "calibre:series", metadata.getSeriesName());
            putIfValid(otherProps, "calibre:series_index", toStringSafe(metadata.getSeriesNumber()));
            putIfValid(otherProps, "booklore:series", metadata.getSeriesName());
            putIfValid(otherProps, "booklore:series_index", toStringSafe(metadata.getSeriesNumber()));
            putIfValid(otherProps, "booklore:asin", metadata.getAsin());
            putIfValid(otherProps, "booklore:amazon_rating", toStringSafe(metadata.getAmazonRating()));
            putIfValid(otherProps, "booklore:amazon_rating_count", toStringSafe(metadata.getAmazonReviewCount()));
            putIfValid(otherProps, "booklore:goodreads_id", metadata.getGoodreadsId());
            putIfValid(otherProps, "booklore:goodreads_rating", toStringSafe(metadata.getGoodreadsRating()));
            putIfValid(otherProps, "booklore:goodreads_rating_count", toStringSafe(metadata.getGoodreadsReviewCount()));
            putIfValid(otherProps, "booklore:hardcover_id", metadata.getHardcoverId());
            putIfValid(otherProps, "booklore:hardcover_rating", toStringSafe(metadata.getHardcoverRating()));
            putIfValid(otherProps, "booklore:hardcover_rating_count", toStringSafe(metadata.getHardcoverReviewCount()));
            putIfValid(otherProps, "booklore:google_books_id", metadata.getGoogleId());
            putIfValid(otherProps, "booklore:page_count", toStringSafe(metadata.getPageCount()));

            meta.setOtherProperties(otherProps);

            if (StringUtils.isNotBlank(metadata.getThumbnail())) {
                File coverFile = new File(metadata.getThumbnail());
                if (coverFile.exists()) {
                    try (FileInputStream coverStream = new FileInputStream(coverFile)) {
                        byte[] coverData = coverStream.readAllBytes();
                        String coverId = "cover-image";

                        Resource cover = new Resource(coverData, "images/cover.jpg");
                        cover.setId(coverId);

                        book.getResources().remove("images/cover.jpg");
                        book.getResources().add(cover);
                        book.setCoverImage(cover);

                        log.info("Cover image replaced for EPUB: {}", coverFile.getName());
                    } catch (IOException e) {
                        log.warn("Failed to read new cover image: {}", e.getMessage());
                    }
                } else {
                    log.warn("Cover path set but file not found: {}", metadata.getThumbnail());
                }
            }

            File tempFile = new File(epubFile.getParentFile(), epubFile.getName() + ".tmp");
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                new EpubWriter().write(book, fos);
            }

            if (!epubFile.delete()) {
                throw new IOException("Could not delete original EPUB file");
            }
            if (!tempFile.renameTo(epubFile)) {
                throw new IOException("Could not rename temp EPUB file");
            }

            log.info("Successfully wrote embedded metadata to EPUB: {}", epubFile.getName());

        } catch (Exception e) {
            log.warn("Failed to write metadata to EPUB file {}: {}", epubFile.getName(), e.getMessage(), e);
        }
    }

    private void putIfValid(Map<QName, String> map, String localName, String value) {
        if (StringUtils.isNotBlank(value)) {
            map.put(new QName(OPF_NS, localName), value);
        }
    }

    private String toStringSafe(Object obj) {
        if (obj == null) return null;
        String str = String.valueOf(obj);
        return StringUtils.isNotBlank(str) && !"null".equalsIgnoreCase(str) ? str : null;
    }
}