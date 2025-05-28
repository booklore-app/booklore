package com.adityachandel.booklore.service.fileprocessor;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.adityachandel.booklore.mapper.BookMapper;
import com.adityachandel.booklore.model.dto.Book;
import com.adityachandel.booklore.model.dto.settings.LibraryFile;
import com.adityachandel.booklore.model.entity.BookEntity;
import com.adityachandel.booklore.model.entity.BookMetadataEntity;
import com.adityachandel.booklore.model.enums.BookFileType;
import com.adityachandel.booklore.repository.BookMetadataRepository;
import com.adityachandel.booklore.repository.BookRepository;
import com.adityachandel.booklore.service.BookCreatorService;
import com.adityachandel.booklore.util.FileUtils;

import io.documentnode.epub4j.domain.Identifier;
import io.documentnode.epub4j.domain.Metadata;
import io.documentnode.epub4j.domain.Resource;
import io.documentnode.epub4j.epub.EpubReader;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.FileHeader;

@Slf4j
@Service
@AllArgsConstructor
public class EpubProcessor implements FileProcessor {

    private final BookRepository bookRepository;
    private final BookCreatorService bookCreatorService;
    private final BookMapper bookMapper;
    private final FileProcessingUtils fileProcessingUtils;
    private final BookMetadataRepository bookMetadataRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public Book processFile(LibraryFile libraryFile, boolean forceProcess) {
        File bookFile = new File(libraryFile.getFileName());
        String fileName = bookFile.getName();
        if (!forceProcess) {
            Optional<BookEntity> bookOptional = bookRepository.findBookByFileNameAndLibraryId(fileName, libraryFile.getLibraryEntity().getId());
            return bookOptional
                    .map(bookMapper::toBook)
                    .orElseGet(() -> processNewFile(libraryFile));
        } else {
            return processNewFile(libraryFile);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected Book processNewFile(LibraryFile libraryFile) {
        BookEntity bookEntity = bookCreatorService.createShellBook(libraryFile, BookFileType.EPUB);
        setBookMetadata(bookEntity);
        if (generateCover(bookEntity)) {
            fileProcessingUtils.setBookCoverPath(bookEntity.getId(), bookEntity.getMetadata());
        }
        bookCreatorService.saveConnections(bookEntity);
        bookRepository.save(bookEntity);
        bookRepository.flush();
        return bookMapper.toBook(bookEntity);
    }

    public boolean generateCover(BookEntity bookEntity) {
        try {
            File epubFile = new File(FileUtils.getBookFullPath(bookEntity));
            io.documentnode.epub4j.domain.Book epub = new EpubReader().readEpub(new FileInputStream(epubFile));

            // Try the default method
            Resource coverImage = epub.getCoverImage();

            // Fallback: look for a manifest resource with common "cover" keywords
            if (coverImage == null) {
                for (Resource res : epub.getResources().getAll()) {
                    String id = res.getId();
                    String href = res.getHref();

                    if ((id != null && id.toLowerCase().contains("cover")) ||
                        (href != null && href.toLowerCase().contains("cover"))) {

                        if (res.getMediaType() != null && res.getMediaType().getName().startsWith("image")) {
                            coverImage = res;
                            break;
                        }
                    }
                }
            }

            boolean saved = saveCoverImage(coverImage, bookEntity.getId());
            bookEntity.getMetadata().setCoverUpdatedOn(Instant.now());
            bookMetadataRepository.save(bookEntity.getMetadata());
            return saved;

        } catch (Exception e) {
            log.error("Error generating cover for epub file {}, error: {}", bookEntity.getFileName(), e.getMessage(), e);
        }
        return false;
    }

    private static Set<String> getAuthors(io.documentnode.epub4j.domain.Book book) {
        return book.getMetadata().getAuthors().stream()
                .map(author -> author.getFirstname() + " " + author.getLastname())
                .collect(Collectors.toSet());
    }

    private void setBookMetadata(BookEntity bookEntity) {
        log.debug("***Setting metadata for book {}", bookEntity.getFileName());
        try (FileInputStream fis =
                new FileInputStream(FileUtils.getBookFullPath(bookEntity))) {

            io.documentnode.epub4j.domain.Book book =
                new EpubReader().readEpub(fis);

            BookMetadataEntity bookMetadata = bookEntity.getMetadata();
            Metadata            epubMetadata = book.getMetadata();

            if (epubMetadata != null) {
                bookMetadata.setTitle(truncate(epubMetadata.getFirstTitle(), 1000));

                if (epubMetadata.getDescriptions() != null && !epubMetadata.getDescriptions().isEmpty()) {
                    bookMetadata.setDescription(truncate(epubMetadata.getDescriptions().getFirst(), 2000));
                }

                if (epubMetadata.getPublishers() != null && !epubMetadata.getPublishers().isEmpty()) {
                    bookMetadata.setPublisher(truncate(epubMetadata.getPublishers().getFirst(), 2000));
                }

                List<String> identifiers = epubMetadata.getIdentifiers().stream()
                        .map(Identifier::getValue)
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .map(id -> id.contains(":") ? id.substring(id.indexOf(":") + 1) : id) // strip prefix
                        .map(id -> id.replace("-", "")) // remove dashes
                        .toList();


                if (!identifiers.isEmpty()) {
                    // Checking for ISBN 13 (strict 13-digit numeric)
                    String isbn13 = identifiers.stream()
                            .filter(id -> id.length() == 13 && id.matches("\\d{13}"))
                            .findFirst()
                            .orElse(null);

                    // Checking for ISBN 10 (strict 10-digit format or ends in X)
                    String isbn10 = identifiers.stream()
                            .filter(id -> id.length() == 10 && id.matches("\\d{9}[\\dXx]"))
                            .findFirst()
                            .orElse(null);

                    // Checking for ASIN (alphanumeric 10 chars, must not be ISBN-10)
                    String asin = identifiers.stream()
                            .filter(id -> id.length() == 10 && id.matches("^[A-Z0-9]{10}$"))
                            .filter(id -> !id.equalsIgnoreCase(isbn10))
                            .findFirst()
                            .orElse(null);

                    bookMetadata.setIsbn13(truncate(isbn13, 64));
                    bookMetadata.setIsbn10(truncate(isbn10, 64));
                    bookMetadata.setAsin(truncate(asin, 20));
                }

                bookMetadata.setLanguage(truncate(
                        epubMetadata.getLanguage() == null || epubMetadata.getLanguage().equalsIgnoreCase("UND") ? "en" : epubMetadata.getLanguage(), 1000
                ));

                if (epubMetadata.getDates() != null && !epubMetadata.getDates().isEmpty()) {
                    epubMetadata.getDates().stream()
                            .findFirst()
                            .ifPresent(publishedDate -> {
                                String dateString = publishedDate.getValue();
                                if (isValidLocalDate(dateString)) {
                                    LocalDate parsedDate = LocalDate.parse(dateString);
                                    bookMetadata.setPublishedDate(parsedDate);
                                } else if (isValidOffsetDateTime(dateString)) {
                                    OffsetDateTime offsetDateTime = OffsetDateTime.parse(dateString);
                                    bookMetadata.setPublishedDate(offsetDateTime.toLocalDate());
                                } else {
                                    log.error("Unable to parse date: {}", dateString);
                                }
                            });
                }
                
                // Calibre (EPUB2) series tags
                String seriesName = epubMetadata.getMetaAttribute("calibre:series");
                if (seriesName != null && !seriesName.isEmpty()) {
                    bookMetadata.setSeriesName(truncate(seriesName, 1000));
                }

                String seriesIndex = epubMetadata.getMetaAttribute("calibre:series_index");
                if (seriesIndex != null && !seriesIndex.isEmpty()) {
                    try {
                        double indexValue = Double.parseDouble(seriesIndex);
                        bookMetadata.setSeriesNumber((float) indexValue);
                    } catch (NumberFormatException e) {
                        log.warn("Unable to parse series number: {}", seriesIndex);
                    }
                }

                //  fall-back to OPF for anything still missing
                extractFromOpf(FileUtils.getBookFullPath(bookEntity), bookMetadata);

                bookCreatorService.addAuthorsToBook(getAuthors(book), bookEntity);
                
                // Get subjects and filter out invalid ones
                List<String> subjects = epubMetadata.getSubjects();
                List<String> validSubjects = new ArrayList<>();
                
                if (subjects != null && !subjects.isEmpty()) {
                    for (String subject : subjects) {
                        // Skip null or empty subjects
                        if (subject == null || subject.trim().isEmpty()) {
                            continue;
                        }
                        
                        // Skip subjects that are too long (likely not real categories)
                        // Real categories are typically short, descriptive terms
                        if (subject.length() > 100) {
                            log.warn("Skipping suspiciously long subject in {}: length={}, value='{}'",
                                    bookEntity.getFileName(), subject.length(),
                                    subject.substring(0, Math.min(50, subject.length())) + "...");
                            continue;
                        }
                        
                        // Skip subjects that contain newlines or excessive whitespace (likely descriptions)
                        if (subject.contains("\n") || subject.contains("\r") || subject.contains("  ")) {
                            log.warn("Skipping subject with newlines/whitespace in {}: '{}'",
                                    bookEntity.getFileName(),
                                    subject.substring(0, Math.min(50, subject.length())) + "...");
                            continue;
                        }

                        validSubjects.add(subject);
                    }
                }
                
                bookCreatorService.addCategoriesToBook(validSubjects, bookEntity);
            }
        } catch (Exception e) {
            log.error("Error loading epub file {}, error: {}", bookEntity.getFileName(), e.getMessage());
        }
    }

    private boolean saveCoverImage(Resource coverImage, long bookId) throws IOException {
        BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(coverImage.getData()));
        return fileProcessingUtils.saveCoverImage(originalImage, bookId);
    }

    private boolean isValidLocalDate(String dateString) {
        try {
            LocalDate.parse(dateString);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private boolean isValidOffsetDateTime(String dateString) {
        try {
            OffsetDateTime.parse(dateString);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private String truncate(String input, int maxLength) {
        if (input == null) return null;
        return input.length() <= maxLength ? input : input.substring(0, maxLength);
    }

    private void extractFromOpf(String epubPath, BookMetadataEntity meta) {
        try (ZipFile zip = new ZipFile(epubPath)) {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            DocumentBuilder builder = dbf.newDocumentBuilder();

            FileHeader containerHdr = zip.getFileHeader("META-INF/container.xml");
            if (containerHdr == null) return;
            try (InputStream cis = zip.getInputStream(containerHdr)) {
                Document containerDoc;
                try {
                    containerDoc = builder.parse(cis);
                } catch (Exception e) {
                    return;
                }
                if (containerDoc == null) return;
                NodeList roots = containerDoc.getElementsByTagName("rootfile");
                if (roots.getLength() == 0) return;
                String opfPath = ((Element) roots.item(0)).getAttribute("full-path");
                if (StringUtils.isBlank(opfPath)) return;
                FileHeader opfHdr = zip.getFileHeader(opfPath);
                if (opfHdr == null) return;
                try (InputStream in = zip.getInputStream(opfHdr)) {
                    Document doc;
                    try {
                        doc = builder.parse(in);
                    } catch (Exception e) {
                        return;
                    }
                    if (doc == null) return;
                    if (StringUtils.isBlank(meta.getIsbn13()) || StringUtils.isBlank(meta.getIsbn10())) {
                        NodeList idNodes = doc.getElementsByTagNameNS("*", "identifier");
                        for (int i = 0; i < idNodes.getLength(); i++) {
                            String idRaw = idNodes.item(i).getTextContent().trim();
                            String isbn = idRaw.toLowerCase().startsWith("isbn:") ? idRaw.substring(5) : idRaw;
                            if (isbn.length() == 13 && StringUtils.isBlank(meta.getIsbn13())) {
                                meta.setIsbn13(truncate(isbn, 64));
                            }
                            if (isbn.length() == 10 && StringUtils.isBlank(meta.getIsbn10())) {
                                meta.setIsbn10(truncate(isbn, 64));
                            }
                        }
                    }
                    NodeList metaNodes = doc.getElementsByTagNameNS("*", "meta");
                    for (int i = 0; i < metaNodes.getLength(); i++) {
                        Element m = (Element) metaNodes.item(i);
                        String nameAttr = m.getAttribute("name");
                        String propAttr = m.getAttribute("property");
                        String content = m.hasAttribute("content") ? m.getAttribute("content").trim() : m.getTextContent().trim();

                        if (StringUtils.isBlank(meta.getSeriesName()) && (nameAttr.equalsIgnoreCase("calibre:series") || propAttr.equalsIgnoreCase("belongs-to-collection"))) {
                            meta.setSeriesName(truncate(content, 1000));
                        }
                        if (meta.getSeriesNumber() == null && (nameAttr.equalsIgnoreCase("calibre:series_index") || propAttr.equalsIgnoreCase("group-position"))) {
                            try {
                                int number = (int) Double.parseDouble(content);
                                meta.setSeriesNumber((float) number);
                            } catch (NumberFormatException ignored) {
                            }
                        }
                        if (meta.getPageCount() == null && (nameAttr.equalsIgnoreCase("calibre:pages") || nameAttr.equalsIgnoreCase("pageCount") || propAttr.equalsIgnoreCase("schema:pageCount") || propAttr.equalsIgnoreCase("media:pageCount"))) {
                            try {
                                int pages = Integer.parseInt(content);
                                meta.setPageCount(pages);
                            } catch (NumberFormatException ignored) {
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }
}