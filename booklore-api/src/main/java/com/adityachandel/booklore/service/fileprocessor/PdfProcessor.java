package com.adityachandel.booklore.service.fileprocessor;

import com.adityachandel.booklore.mapper.BookMapper;
import com.adityachandel.booklore.model.dto.settings.LibraryFile;
import com.adityachandel.booklore.model.dto.Book;
import com.adityachandel.booklore.model.entity.BookEntity;
import com.adityachandel.booklore.model.enums.BookFileType;
import com.adityachandel.booklore.repository.BookMetadataRepository;
import com.adityachandel.booklore.repository.BookRepository;
import com.adityachandel.booklore.service.BookCreatorService;
import com.adityachandel.booklore.util.FileUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class PdfProcessor implements FileProcessor {

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
        BookEntity bookEntity = bookCreatorService.createShellBook(libraryFile, BookFileType.PDF);
        if (generateCover(bookEntity)) {
            fileProcessingUtils.setBookCoverPath(bookEntity.getId(), bookEntity.getMetadata());
        }
        setMetadata(bookEntity);
        bookCreatorService.saveConnections(bookEntity);
        bookEntity = bookRepository.save(bookEntity);
        bookRepository.flush();
        return bookMapper.toBook(bookEntity);
    }

    public boolean generateCover(BookEntity bookEntity) {
        try (PDDocument pdf = Loader.loadPDF(new File(FileUtils.getBookFullPath(bookEntity)))) {
            boolean saved = generateCoverImageAndSave(bookEntity.getId(), pdf);
            bookEntity.getMetadata().setCoverUpdatedOn(Instant.now());
            bookMetadataRepository.save(bookEntity.getMetadata());
            return saved;
        } catch (Exception e) {
            log.error("Error generating cover for pdf file {}, error: {}", bookEntity.getFileName(), e.getMessage());
        }
        return false;
    }

    private void setMetadata(BookEntity bookEntity) {
        try (PDDocument pdf = Loader.loadPDF(new File(FileUtils.getBookFullPath(bookEntity)))) {
            if (pdf.getDocumentInformation() == null) {
                log.warn("No document information found");
            } else {
                if (pdf.getDocumentInformation().getTitle() != null) {
                    bookEntity.getMetadata().setTitle(truncate(pdf.getDocumentInformation().getTitle(), 1000));
                }
                if (pdf.getDocumentInformation().getAuthor() != null) {
                    Set<String> authors = getAuthors(pdf);
                    bookCreatorService.addAuthorsToBook(authors, bookEntity);
                }
            }
        } catch (Exception e) {
            log.error("Error loading pdf file {}, error: {}", bookEntity.getFileName(), e.getMessage());
        }
    }

    private Set<String> getAuthors(PDDocument document) {
        String authorNamesUnsplit = document.getDocumentInformation().getAuthor();
        Set<String> authorNames = new HashSet<>();
        if (authorNamesUnsplit.contains("&")) {
            authorNames.addAll(Arrays.asList(authorNamesUnsplit.split("&")));
        } else if (authorNamesUnsplit.contains(",")) {
            authorNames.addAll(Arrays.asList(authorNamesUnsplit.split(",")));
        } else {
            authorNames.add(authorNamesUnsplit);
        }
        return authorNames.stream().map(String::trim).collect(Collectors.toSet());
    }

    private boolean generateCoverImageAndSave(Long bookId, PDDocument document) throws IOException {
        BufferedImage coverImage = new PDFRenderer(document).renderImageWithDPI(0, 300, ImageType.RGB);
        return fileProcessingUtils.saveCoverImage(coverImage, bookId);
    }

    private String truncate(String input, int maxLength) {
        if (input == null) return null;
        return input.length() <= maxLength ? input : input.substring(0, maxLength);
    }
}
