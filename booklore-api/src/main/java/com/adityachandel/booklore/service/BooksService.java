package com.adityachandel.booklore.service;

import com.adityachandel.booklore.config.security.AuthenticationService;
import com.adityachandel.booklore.exception.ApiError;
import com.adityachandel.booklore.mapper.*;
import com.adityachandel.booklore.model.dto.*;
import com.adityachandel.booklore.model.dto.request.ReadProgressRequest;
import com.adityachandel.booklore.model.entity.*;
import com.adityachandel.booklore.model.enums.BookFileType;
import com.adityachandel.booklore.repository.*;
import com.adityachandel.booklore.util.FileService;
import com.adityachandel.booklore.util.FileUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@AllArgsConstructor
@Service
public class BooksService {

    private final BookRepository bookRepository;
    private final BookMetadataRepository bookMetadataRepository;
    private final PdfViewerPreferencesRepository pdfViewerPreferencesRepository;
    private final EpubViewerPreferencesRepository epubViewerPreferencesRepository;
    private final CbxViewerPreferencesRepository cbxViewerPreferencesRepository;
    private final NewPdfViewerPreferencesRepository newPdfViewerPreferencesRepository;
    private final ShelfRepository shelfRepository;
    private final FileService fileService;
    private final BookMapper bookMapper;
    private final UserRepository userRepository;
    private final UserBookProgressRepository userBookProgressRepository;
    private final AuthenticationService authenticationService;
    private final AuthorRepository authorRepository;
    private final CategoryRepository categoryRepository;
    private final BookMetadataAuthorMappingRepository bookMetadataAuthorMappingRepository;
    private final BookMetadataCategoryMappingRepository bookMetadataCategoryMappingRepository;
    private final BookShelfMappingRepository bookShelfMappingRepository;
    private final BookMetadataMapper bookMetadataMapper;
    private final AuthorMapper authorMapper;
    private final CategoryMapper categoryMapper;
    private final ShelfMapper shelfMapper;


    public BookViewerSettings getBookViewerSetting(long bookId) {
        BookEntity bookEntity = bookRepository.findById(bookId).orElseThrow(() -> ApiError.BOOK_NOT_FOUND.createException(bookId));
        BookLoreUser user = authenticationService.getAuthenticatedUser();
        BookViewerSettings.BookViewerSettingsBuilder settingsBuilder = BookViewerSettings.builder();
        if (bookEntity.getBookType() == BookFileType.EPUB) {
            epubViewerPreferencesRepository.findByBookIdAndUserId(bookId, user.getId())
                    .ifPresent(epubPref -> settingsBuilder.epubSettings(EpubViewerPreferences.builder()
                            .bookId(bookId)
                            .font(epubPref.getFont())
                            .fontSize(epubPref.getFontSize())
                            .theme(epubPref.getTheme())
                            .flow(epubPref.getFlow())
                            .build()));
        } else if (bookEntity.getBookType() == BookFileType.PDF) {
            pdfViewerPreferencesRepository.findByBookIdAndUserId(bookId, user.getId())
                    .ifPresent(pdfPref -> settingsBuilder.pdfSettings(PdfViewerPreferences.builder()
                            .bookId(bookId)
                            .zoom(pdfPref.getZoom())
                            .spread(pdfPref.getSpread())
                            .build()));
            newPdfViewerPreferencesRepository.findByBookIdAndUserId(bookId, user.getId())
                    .ifPresent(pdfPref -> settingsBuilder.newPdfSettings(NewPdfViewerPreferences.builder()
                            .bookId(bookId)
                            .pageViewMode(pdfPref.getPageViewMode())
                            .pageSpread(pdfPref.getPageSpread())
                            .build()));
        } else if (bookEntity.getBookType() == BookFileType.CBX) {
            cbxViewerPreferencesRepository.findByBookIdAndUserId(bookId, user.getId())
                    .ifPresent(cbxPref -> settingsBuilder.cbxSettings(CbxViewerPreferences.builder()
                            .bookId(bookId)
                            .pageViewMode(cbxPref.getPageViewMode())
                            .pageSpread(cbxPref.getPageSpread())
                            .build()));
        } else {
            throw ApiError.UNSUPPORTED_BOOK_TYPE.createException();
        }
        return settingsBuilder.build();
    }

    public void updateBookViewerSetting(long bookId, BookViewerSettings bookViewerSettings) {
        BookEntity bookEntity = bookRepository.findById(bookId).orElseThrow(() -> ApiError.BOOK_NOT_FOUND.createException(bookId));
        BookLoreUser user = authenticationService.getAuthenticatedUser();

        if (bookEntity.getBookType() == BookFileType.PDF) {
            if (bookViewerSettings.getPdfSettings() != null) {
                PdfViewerPreferencesEntity pdfPrefs = pdfViewerPreferencesRepository
                        .findByBookIdAndUserId(bookId, user.getId())
                        .orElseGet(() -> {
                            PdfViewerPreferencesEntity newPrefs = PdfViewerPreferencesEntity.builder()
                                    .bookId(bookId)
                                    .userId(user.getId())
                                    .build();
                            return pdfViewerPreferencesRepository.save(newPrefs);
                        });
                PdfViewerPreferences pdfSettings = bookViewerSettings.getPdfSettings();
                pdfPrefs.setZoom(pdfSettings.getZoom());
                pdfPrefs.setSpread(pdfSettings.getSpread());
                pdfViewerPreferencesRepository.save(pdfPrefs);
            }
            if (bookViewerSettings.getNewPdfSettings() != null) {
                NewPdfViewerPreferencesEntity pdfPrefs = newPdfViewerPreferencesRepository.findByBookIdAndUserId(bookId, user.getId())
                        .orElseGet(() -> {
                            NewPdfViewerPreferencesEntity entity = NewPdfViewerPreferencesEntity.builder()
                                    .bookId(bookId)
                                    .userId(user.getId())
                                    .build();
                            return newPdfViewerPreferencesRepository.save(entity);
                        });
                NewPdfViewerPreferences pdfSettings = bookViewerSettings.getNewPdfSettings();
                pdfPrefs.setPageSpread(pdfSettings.getPageSpread());
                pdfPrefs.setPageViewMode(pdfSettings.getPageViewMode());
                newPdfViewerPreferencesRepository.save(pdfPrefs);
            }
        } else if (bookEntity.getBookType() == BookFileType.EPUB) {
            EpubViewerPreferencesEntity epubPrefs = epubViewerPreferencesRepository
                    .findByBookIdAndUserId(bookId, user.getId())
                    .orElseGet(() -> {
                        EpubViewerPreferencesEntity newPrefs = EpubViewerPreferencesEntity.builder()
                                .bookId(bookId)
                                .userId(user.getId())
                                .build();
                        return epubViewerPreferencesRepository.save(newPrefs);
                    });

            EpubViewerPreferences epubSettings = bookViewerSettings.getEpubSettings();
            epubPrefs.setFont(epubSettings.getFont());
            epubPrefs.setFontSize(epubSettings.getFontSize());
            epubPrefs.setTheme(epubSettings.getTheme());
            epubPrefs.setFlow(epubSettings.getFlow());
            epubViewerPreferencesRepository.save(epubPrefs);

        } else if (bookEntity.getBookType() == BookFileType.CBX) {
            CbxViewerPreferencesEntity cbxPrefs = cbxViewerPreferencesRepository
                    .findByBookIdAndUserId(bookId, user.getId())
                    .orElseGet(() -> {
                        CbxViewerPreferencesEntity newPrefs = CbxViewerPreferencesEntity.builder()
                                .bookId(bookId)
                                .userId(user.getId())
                                .build();
                        return cbxViewerPreferencesRepository.save(newPrefs);
                    });

            CbxViewerPreferences cbxSettings = bookViewerSettings.getCbxSettings();
            cbxPrefs.setPageSpread(cbxSettings.getPageSpread());
            cbxPrefs.setPageViewMode(cbxSettings.getPageViewMode());
            cbxViewerPreferencesRepository.save(cbxPrefs);

        } else {
            throw ApiError.UNSUPPORTED_BOOK_TYPE.createException();
        }
    }

    public Book getBook(long bookId, boolean withDescription) {
        BookLoreUser user = authenticationService.getAuthenticatedUser();
        BookEntity bookEntity = bookRepository.findById(bookId).orElseThrow(() -> ApiError.BOOK_NOT_FOUND.createException(bookId));

        UserBookProgressEntity userProgress = userBookProgressRepository.findByUserIdAndBookId(user.getId(), bookId).orElse(new UserBookProgressEntity());

        Book book = bookMapper.toBook(bookEntity);
        book.setLastReadTime(userProgress.getLastReadTime());

        if (bookEntity.getBookType() == BookFileType.PDF) {
            book.setPdfProgress(PdfProgress.builder()
                    .page(userProgress.getPdfProgress())
                    .percentage(userProgress.getPdfProgressPercent())
                    .build());
        }
        if (bookEntity.getBookType() == BookFileType.EPUB) {
            book.setEpubProgress(EpubProgress.builder()
                    .cfi(userProgress.getEpubProgress())
                    .percentage(userProgress.getEpubProgressPercent())
                    .build());
        }
        if (bookEntity.getBookType() == BookFileType.CBX) {
            book.setCbxProgress(CbxProgress.builder()
                    .page(userProgress.getCbxProgress())
                    .percentage(userProgress.getCbxProgressPercent())
                    .build());
        }
        book.setFilePath(FileUtils.getBookFullPath(bookEntity));

        if (!withDescription) {
            book.getMetadata().setDescription(null);
        }

        return book;
    }

    public List<Book> getBooks(boolean withDescription) {
        BookLoreUser user = authenticationService.getAuthenticatedUser();

        Set<Long> bookIds;
        List<BookEntity> bookEntities = user.getPermissions().isAdmin()
                ? bookRepository.getAllBooks()
                : bookRepository.getAllByLibraryIds(
                user.getAssignedLibraries().stream()
                        .map(Library::getId)
                        .collect(Collectors.toSet())
        );

        List<Book> books = bookEntities.stream()
                .map(bookMapper::toBookWithoutMetadataAndShelves)
                .toList();

        bookIds = bookEntities.stream()
                .map(BookEntity::getId)
                .collect(Collectors.toSet());

        Map<Long, BookMetadata> metadataByBookId = fetchMetadata(bookIds, withDescription);
        Map<Long, List<String>> authorsByBookId = fetchAuthors(bookIds);
        Map<Long, List<String>> categoriesByBookId = fetchCategories(bookIds);
        Map<Long, List<Shelf>> shelvesByBookId = fetchShelves(bookIds);
        Map<Long, UserBookProgressEntity> progressByBookId = fetchUserProgress(user.getId(), bookIds);

        for (Book book : books) {
            Long bookId = book.getId();

            var metadata = metadataByBookId.get(bookId);
            if (metadata != null) {
                metadata.setAuthors(authorsByBookId.getOrDefault(bookId, List.of()));
                metadata.setCategories(categoriesByBookId.getOrDefault(bookId, List.of()));
                book.setMetadata(metadata);
            }

            book.setShelves(shelvesByBookId.getOrDefault(bookId, List.of()));

            var progress = progressByBookId.get(bookId);
            if (progress != null) {
                book.setLastReadTime(progress.getLastReadTime());
                setBookProgress(book, progress);
            }
        }

        return books;
    }

    private Map<Long, BookMetadata> fetchMetadata(Set<Long> bookIds, boolean withDescription) {
        var metadataList = bookMetadataRepository.getMetadataForBookIds(bookIds);
        return metadataList.stream()
                .map(m -> bookMetadataMapper.toBookMetadataWithoutRelations(m, withDescription))
                .collect(Collectors.toMap(BookMetadata::getBookId, m -> m));
    }

    private Map<Long, List<String>> fetchAuthors(Set<Long> bookIds) {
        var mappings = bookMetadataAuthorMappingRepository.findAllByBookIdIn(bookIds);
        var authorIds = mappings.stream()
                .map(BookMetadataAuthorMapping::getAuthorId)
                .collect(Collectors.toSet());
        var authors = authorRepository.findAllByIdIn(authorIds);
        var authorNamesById = authors.stream()
                .collect(Collectors.toMap(AuthorEntity::getId, authorMapper::toAuthorEntityName));

        return mappings.stream()
                .collect(Collectors.groupingBy(
                        BookMetadataAuthorMapping::getBookId,
                        Collectors.mapping(m -> authorNamesById.get(m.getAuthorId()), Collectors.toList())
                ));
    }

    private Map<Long, List<String>> fetchCategories(Set<Long> bookIds) {
        var mappings = bookMetadataCategoryMappingRepository.findAllByBookIdIn(bookIds);
        var categoryIds = mappings.stream()
                .map(BookMetadataCategoryMapping::getCategoryId)
                .collect(Collectors.toSet());
        var categories = categoryRepository.findAllByIdIn(categoryIds);
        var categoryNamesById = categories.stream()
                .collect(Collectors.toMap(CategoryEntity::getId, categoryMapper::toCategoryName));

        return mappings.stream()
                .collect(Collectors.groupingBy(
                        BookMetadataCategoryMapping::getBookId,
                        Collectors.mapping(m -> categoryNamesById.get(m.getCategoryId()), Collectors.toList())
                ));
    }

    private Map<Long, List<Shelf>> fetchShelves(Set<Long> bookIds) {
        var shelfMappings = bookShelfMappingRepository.findAllByBookIdIn(bookIds);
        var shelfIds = shelfMappings.stream()
                .map(BookShelfMapping::getShelfId)
                .collect(Collectors.toSet());
        var shelves = shelfRepository.findAllByIdIn(shelfIds);
        var shelfById = shelves.stream()
                .collect(Collectors.toMap(ShelfEntity::getId, shelfMapper::toShelf));

        return shelfMappings.stream()
                .collect(Collectors.groupingBy(
                        BookShelfMapping::getBookId,
                        Collectors.mapping(m -> shelfById.get(m.getShelfId()), Collectors.toList())
                ));
    }

    private Map<Long, UserBookProgressEntity> fetchUserProgress(Long userId, Set<Long> bookIds) {
        return userBookProgressRepository.findByUserIdAndBookIdIn(userId, bookIds).stream()
                .collect(Collectors.toMap(p -> p.getBook().getId(), p -> p));
    }

    private void setBookProgress(Book book, UserBookProgressEntity progress) {
        switch (book.getBookType()) {
            case EPUB -> book.setEpubProgress(EpubProgress.builder()
                    .cfi(progress.getEpubProgress())
                    .percentage(progress.getEpubProgressPercent())
                    .build());
            case PDF -> book.setPdfProgress(PdfProgress.builder()
                    .page(progress.getPdfProgress())
                    .percentage(progress.getPdfProgressPercent())
                    .build());
            case CBX -> book.setCbxProgress(CbxProgress.builder()
                    .page(progress.getCbxProgress())
                    .percentage(progress.getCbxProgressPercent())
                    .build());
        }
    }


    @Transactional
    public List<Book> assignShelvesToBooks(Set<Long> bookIds, Set<Long> shelfIdsToAssign, Set<Long> shelfIdsToUnassign) {
        BookLoreUser user = authenticationService.getAuthenticatedUser();
        BookLoreUserEntity userEntity = userRepository.findById(user.getId()).orElseThrow(() -> ApiError.USER_NOT_FOUND.createException(user.getId()));

        Set<Long> userShelfIds = userEntity.getShelves().stream()
                .map(ShelfEntity::getId)
                .collect(Collectors.toSet());

        if (!userShelfIds.containsAll(shelfIdsToAssign)) {
            throw ApiError.UNAUTHORIZED.createException("Cannot assign shelves that do not belong to the user.");
        }
        if (!userShelfIds.containsAll(shelfIdsToUnassign)) {
            throw ApiError.UNAUTHORIZED.createException("Cannot unassign shelves that do not belong to the user.");
        }

        List<BookEntity> bookEntities = bookRepository.findAllById(bookIds);
        List<ShelfEntity> shelvesToAssign = shelfRepository.findAllById(shelfIdsToAssign);
        for (BookEntity bookEntity : bookEntities) {
            bookEntity.getShelves().removeIf(shelf -> shelfIdsToUnassign.contains(shelf.getId()));

            for (ShelfEntity shelf : shelvesToAssign) {
                if (!bookEntity.getShelves().contains(shelf)) {
                    bookEntity.getShelves().add(shelf);
                }
            }
        }
        bookRepository.saveAll(bookEntities);
        return bookEntities.stream().map(bookMapper::toBook).collect(Collectors.toList());
    }

    public Resource getBookCover(long bookId) {
        BookEntity bookEntity = bookRepository.findById(bookId).orElseThrow(() -> ApiError.BOOK_NOT_FOUND.createException(bookId));
        return fileService.getBookCover(bookEntity.getMetadata().getThumbnail());
    }

    @Transactional
    public void updateReadProgress(ReadProgressRequest request) {
        BookEntity book = bookRepository.findById(request.getBookId()).orElseThrow(() -> ApiError.BOOK_NOT_FOUND.createException(request.getBookId()));
        BookLoreUser user = authenticationService.getAuthenticatedUser();
        UserBookProgressEntity userBookProgress = userBookProgressRepository.findByUserIdAndBookId(user.getId(), book.getId()).orElse(new UserBookProgressEntity());
        userBookProgress.setUser(userRepository.findById(user.getId()).orElseThrow(() -> new UsernameNotFoundException("User not found")));
        userBookProgress.setBook(book);
        userBookProgress.setLastReadTime(Instant.now());
        if (book.getBookType() == BookFileType.EPUB && request.getEpubProgress() != null) {
            userBookProgress.setEpubProgress(request.getEpubProgress().getCfi());
            userBookProgress.setEpubProgressPercent(request.getEpubProgress().getPercentage());
        } else if (book.getBookType() == BookFileType.PDF && request.getPdfProgress() != null) {
            userBookProgress.setPdfProgress(request.getPdfProgress().getPage());
            userBookProgress.setPdfProgressPercent(request.getPdfProgress().getPercentage());
        } else if (book.getBookType() == BookFileType.CBX && request.getCbxProgress() != null) {
            userBookProgress.setCbxProgress(request.getCbxProgress().getPage());
            userBookProgress.setCbxProgressPercent(request.getCbxProgress().getPercentage());
        }
        userBookProgressRepository.save(userBookProgress);
    }

    public ResponseEntity<Resource> downloadBook(Long bookId) {
        try {
            BookEntity bookEntity = bookRepository.findById(bookId).orElseThrow(() -> ApiError.BOOK_NOT_FOUND.createException(bookId));
            Path file = Paths.get(FileUtils.getBookFullPath(bookEntity)).toAbsolutePath().normalize();
            Resource resource = new UrlResource(file.toUri());
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFileName() + "\"")
                    .body(resource);
        } catch (Exception e) {
            throw ApiError.FAILED_TO_DOWNLOAD_FILE.createException(bookId);
        }
    }

    public ResponseEntity<ByteArrayResource> getBookContent(long bookId) throws IOException {
        BookEntity bookEntity = bookRepository.findById(bookId).orElseThrow(() -> ApiError.BOOK_NOT_FOUND.createException(bookId));
        try (FileInputStream inputStream = new FileInputStream(FileUtils.getBookFullPath(bookEntity))) {
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(new ByteArrayResource(inputStream.readAllBytes()));
        }
    }
}