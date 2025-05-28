package com.adityachandel.booklore.service;

import com.adityachandel.booklore.model.dto.settings.LibraryFile;
import com.adityachandel.booklore.model.entity.*;
import com.adityachandel.booklore.model.enums.BookFileType;
import com.adityachandel.booklore.repository.*;
import com.adityachandel.booklore.util.FileUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@AllArgsConstructor
public class BookCreatorService {

    private final AuthorRepository authorRepository;
    private final CategoryRepository categoryRepository;
    private final BookRepository bookRepository;
    private final BookMetadataRepository bookMetadataRepository;

    public BookEntity createShellBook(LibraryFile libraryFile, BookFileType bookFileType) {
        long fileSizeKb = FileUtils.getFileSizeInKb(libraryFile.getFullPath());
        BookEntity bookEntity = BookEntity.builder()
                .library(libraryFile.getLibraryEntity())
                .libraryPath(libraryFile.getLibraryPathEntity())
                .fileName(libraryFile.getFileName())
                .fileSubPath(libraryFile.getFileSubPath())
                .bookType(bookFileType)
                .fileSizeKb(fileSizeKb)
                .addedOn(Instant.now())
                .build();
        BookMetadataEntity bookMetadataEntity = BookMetadataEntity.builder().build();
        bookEntity.setMetadata(bookMetadataEntity);
        return bookRepository.saveAndFlush(bookEntity);
    }

    public void addCategoriesToBook(List<String> categories, BookEntity bookEntity) {
        for (String category : categories) {
            // Truncate category name to fit in VARCHAR(255)
            String truncatedCategory = truncate(category, 255);
            Optional<CategoryEntity> catOpt = categoryRepository.findByName(truncatedCategory);
            CategoryEntity categoryEntity;
            if (catOpt.isPresent()) {
                categoryEntity = catOpt.get();
            } else {
                categoryEntity = CategoryEntity.builder()
                        .name(truncatedCategory)
                        .build();
                categoryEntity = categoryRepository.save(categoryEntity);
            }
            if (bookEntity.getMetadata().getCategories() == null) {
                bookEntity.getMetadata().setCategories(new ArrayList<>());
            }
            bookEntity.getMetadata().getCategories().add(categoryEntity);
        }
    }

    public void addAuthorsToBook(Set<String> authors, BookEntity bookEntity) {
        for (String authorStr : authors) {
            Optional<AuthorEntity> authorOptional = authorRepository.findByName(authorStr);
            AuthorEntity authorEntity;
            if (authorOptional.isPresent()) {
                authorEntity = authorOptional.get();
            } else {
                authorEntity = AuthorEntity.builder()
                        .name(authorStr)
                        .build();
                authorEntity = authorRepository.save(authorEntity);
            }
            if (bookEntity.getMetadata().getAuthors() == null) {
                bookEntity.getMetadata().setAuthors(new ArrayList<>());
            }
            bookEntity.getMetadata().getAuthors().add(authorEntity);
        }
    }
    
    /**
     * Truncates a string to the specified maximum length
     * @param input The input string
     * @param maxLength The maximum length
     * @return The truncated string, or null if input is null
     */
    private String truncate(String input, int maxLength) {
        if (input == null) return null;
        return input.length() <= maxLength ? input : input.substring(0, maxLength);
    }

    public void saveConnections(BookEntity bookEntity) {
        if (bookEntity.getMetadata().getAuthors() != null && !bookEntity.getMetadata().getAuthors().isEmpty()) {
            authorRepository.saveAll(bookEntity.getMetadata().getAuthors());
        }
        bookRepository.save(bookEntity);
        bookMetadataRepository.save(bookEntity.getMetadata());
    }
}
