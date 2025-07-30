package com.adityachandel.booklore.service.fileprocessor;

import com.adityachandel.booklore.mapper.BookMapper;
import com.adityachandel.booklore.model.dto.Book;
import com.adityachandel.booklore.model.dto.settings.LibraryFile;
import com.adityachandel.booklore.model.entity.BookEntity;
import com.adityachandel.booklore.repository.BookMetadataRepository;
import com.adityachandel.booklore.repository.BookRepository;
import com.adityachandel.booklore.service.BookCreatorService;
import com.adityachandel.booklore.service.metadata.MetadataMatchService;
import com.adityachandel.booklore.util.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.util.Optional;

@Slf4j
public abstract class AbstractFileProcessor implements BookFileProcessor {

    protected final BookRepository bookRepository;
    protected final BookCreatorService bookCreatorService;
    protected final BookMapper bookMapper;
    protected final FileProcessingUtils fileProcessingUtils;
    protected final BookMetadataRepository bookMetadataRepository;
    protected final MetadataMatchService metadataMatchService;

    protected AbstractFileProcessor(BookRepository bookRepository, BookCreatorService bookCreatorService, BookMapper bookMapper, FileProcessingUtils fileProcessingUtils, BookMetadataRepository bookMetadataRepository, MetadataMatchService metadataMatchService) {
        this.bookRepository = bookRepository;
        this.bookCreatorService = bookCreatorService;
        this.bookMapper = bookMapper;
        this.fileProcessingUtils = fileProcessingUtils;
        this.bookMetadataRepository = bookMetadataRepository;
        this.metadataMatchService = metadataMatchService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public Book processFile(LibraryFile libraryFile) {
        Path filePath = libraryFile.getFullPath();
        String fileName = filePath.getFileName().toString();
        String hash = FileUtils.computeFileHash(filePath);

        Optional<Book> existing = fileProcessingUtils.checkForDuplicateAndUpdateMetadataIfNeeded(libraryFile, hash, bookRepository, bookMapper);
        if (existing.isPresent()) {
            return existing.get();
        }
        Optional<BookEntity> byName = bookRepository.findBookByFileNameAndLibraryId(fileName, libraryFile.getLibraryEntity().getId());
        if (byName.isPresent()) {
            return bookMapper.toBook(byName.get());
        }

        BookEntity book = processNewFile(libraryFile);
        book.setCurrentHash(hash);

        Float score = metadataMatchService.calculateMatchScore(book);
        book.setMetadataMatchScore(score);

        bookCreatorService.saveConnections(book);

        return bookMapper.toBook(book);
    }

    protected abstract BookEntity processNewFile(LibraryFile libraryFile);

}
