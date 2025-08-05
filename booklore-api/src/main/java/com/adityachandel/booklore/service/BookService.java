package com.adityachandel.booklore.service;

import com.adityachandel.booklore.config.security.AuthenticationService;
import com.adityachandel.booklore.exception.ApiError;
import com.adityachandel.booklore.mapper.BookMapper;
import com.adityachandel.booklore.model.dto.*;
import com.adityachandel.booklore.model.dto.request.ReadProgressRequest;
import com.adityachandel.booklore.model.dto.response.BookDeletionResponse;
import com.adityachandel.booklore.model.entity.*;
import com.adityachandel.booklore.model.enums.BookFileType;
import com.adityachandel.booklore.model.enums.ReadStatus;
import com.adityachandel.booklore.repository.*;
import com.adityachandel.booklore.util.FileService;
import com.adityachandel.booklore.util.FileUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.EnumUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@AllArgsConstructor
@Service
public class BookService {

    private final BookRepository bookRepository;
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
    private final BookQueryService bookQueryService;
    private final UserProgressService userProgressService;

    public List<Book> getBookDTOs(boolean includeDescription) {
        BookLoreUser user = authenticationService.getAuthenticatedUser();
        boolean isAdmin = user.getPermissions().isAdmin();

        List<Book> books = isAdmin
                ? bookQueryService.getAllBooks(includeDescription)
                : bookQueryService.getAllBooksByLibraryIds(
                user.getAssignedLibraries().stream()
                        .map(Library::getId)
                        .collect(Collectors.toSet()),
                includeDescription
        );

        Map<Long, UserBookProgressEntity> progressMap =
                userProgressService.fetchUserProgress(
                        user.getId(),
                        books.stream().map(Book::getId).collect(Collectors.toSet())
                );

        books.forEach(book -> {
            UserBookProgressEntity progress = progressMap.get(book.getId());
            if (progress != null) {
                setBookProgress(book, progress);
                book.setLastReadTime(progress.getLastReadTime());
                book.setReadStatus(String.valueOf(progress.getReadStatus()));
                book.setDateFinished(progress.getDateFinished());
            }
        });

        return books;
    }

    public List<Book> getBooksByIds(Set<Long> bookIds, boolean withDescription) {
        BookLoreUser user = authenticationService.getAuthenticatedUser();

        List<BookEntity> bookEntities;

        bookEntities = bookQueryService.findAllWithMetadataByIds(bookIds);

        Map<Long, UserBookProgressEntity> progressMap = userProgressService.fetchUserProgress(
                user.getId(), bookEntities.stream().map(BookEntity::getId).collect(Collectors.toSet()));

        return bookEntities.stream().map(bookEntity -> {
            Book book = bookMapper.toBook(bookEntity);
            book.setFilePath(FileUtils.getBookFullPath(bookEntity));

            if (!withDescription) {
                book.getMetadata().setDescription(null);
            }

            UserBookProgressEntity progress = progressMap.get(bookEntity.getId());
            if (progress != null) {
                setBookProgress(book, progress);
                book.setLastReadTime(progress.getLastReadTime());
                book.setReadStatus(String.valueOf(progress.getReadStatus()));
                book.setDateFinished(progress.getDateFinished());
            }

            return book;
        }).collect(Collectors.toList());
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
                            .letterSpacing(epubPref.getLetterSpacing())
                            .lineHeight(epubPref.getLineHeight())
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
            epubPrefs.setLetterSpacing(epubSettings.getLetterSpacing());
            epubPrefs.setLineHeight(epubSettings.getLineHeight());
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
        book.setReadStatus(String.valueOf(userProgress.getReadStatus()));
        book.setDateFinished(userProgress.getDateFinished());

        if (!withDescription) {
            book.getMetadata().setDescription(null);
        }

        return book;
    }

    public List<Book> resetProgress(List<Long> bookIds) {
        BookLoreUser user = authenticationService.getAuthenticatedUser();
        List<Book> updatedBooks = new ArrayList<>();
        Optional<BookLoreUserEntity> userEntity = userRepository.findById(user.getId());

        for (Long bookId : bookIds) {
            BookEntity bookEntity = bookRepository.findById(bookId).orElseThrow(() -> ApiError.BOOK_NOT_FOUND.createException(bookId));

            UserBookProgressEntity progress = userBookProgressRepository
                    .findByUserIdAndBookId(user.getId(), bookId)
                    .orElse(new UserBookProgressEntity());

            progress.setBook(bookEntity);
            progress.setUser(userEntity.orElseThrow());
            progress.setReadStatus(null);
            progress.setLastReadTime(null);
            progress.setPdfProgress(null);
            progress.setPdfProgressPercent(null);
            progress.setEpubProgress(null);
            progress.setEpubProgressPercent(null);
            progress.setCbxProgress(null);
            progress.setCbxProgressPercent(null);
            progress.setDateFinished(null);

            userBookProgressRepository.save(progress);
            updatedBooks.add(bookMapper.toBook(bookEntity));
        }

        return updatedBooks;
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

        List<BookEntity> bookEntities = bookQueryService.findAllWithMetadataByIds(bookIds);
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

    @Transactional
    public List<Book> updateReadStatus(List<Long> bookIds, String status) {
        BookLoreUser user = authenticationService.getAuthenticatedUser();
        ReadStatus readStatus = EnumUtils.getEnumIgnoreCase(ReadStatus.class, status);

        List<BookEntity> books = bookRepository.findAllById(bookIds);
        if (books.size() != bookIds.size()) {
            throw ApiError.BOOK_NOT_FOUND.createException("One or more books not found");
        }

        BookLoreUserEntity userEntity = userRepository.findById(user.getId()).orElseThrow(() -> new UsernameNotFoundException("User not found"));
        for (BookEntity book : books) {
            UserBookProgressEntity progress = userBookProgressRepository
                    .findByUserIdAndBookId(user.getId(), book.getId())
                    .orElse(new UserBookProgressEntity());

            progress.setUser(userEntity);
            progress.setBook(book);
            progress.setReadStatus(readStatus);
            
            // Set dateFinished when status is READ, clear it otherwise
            if (readStatus == ReadStatus.READ) {
                progress.setDateFinished(Instant.now());
            } else {
                progress.setDateFinished(null);
            }
            
            userBookProgressRepository.save(progress);
        }
        
        // Return updated books with the latest data
        return books.stream()
                .map(bookEntity -> {
                    Book book = bookMapper.toBook(bookEntity);
                    book.setFilePath(FileUtils.getBookFullPath(bookEntity));
                    
                    UserBookProgressEntity progress = userBookProgressRepository
                            .findByUserIdAndBookId(user.getId(), bookEntity.getId())
                            .orElse(null);
                    
                    if (progress != null) {
                        setBookProgress(book, progress);
                        book.setLastReadTime(progress.getLastReadTime());
                        book.setReadStatus(String.valueOf(progress.getReadStatus()));
                        book.setDateFinished(progress.getDateFinished());
                    }
                    
                    return book;
                })
                .collect(Collectors.toList());
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


    @Transactional
    public ResponseEntity<BookDeletionResponse> deleteBooks(Set<Long> ids) {
        List<BookEntity> books = bookQueryService.findAllWithMetadataByIds(ids);
        List<Long> failedFileDeletions = new ArrayList<>();

        for (BookEntity book : books) {
            Path fullFilePath = book.getFullFilePath();
            try {
                if (Files.exists(fullFilePath)) {
                    Files.delete(fullFilePath);
                    Set<Path> libraryRoots = book.getLibrary().getLibraryPaths().stream()
                            .map(LibraryPathEntity::getPath)
                            .map(Paths::get)
                            .map(Path::normalize)
                            .collect(Collectors.toSet());

                    deleteEmptyParentDirsUpToLibraryFolders(fullFilePath.getParent(), libraryRoots);
                }
            } catch (IOException e) {
                failedFileDeletions.add(book.getId());
            }
        }

        bookRepository.deleteAll(books);
        BookDeletionResponse response = new BookDeletionResponse(ids, failedFileDeletions);
        return failedFileDeletions.isEmpty()
                ? ResponseEntity.ok(response)
                : ResponseEntity.status(HttpStatus.MULTI_STATUS).body(response);
    }

    public void deleteEmptyParentDirsUpToLibraryFolders(Path currentDir, Set<Path> libraryRoots) throws IOException {
        Set<String> ignoredFilenames = Set.of(".DS_Store", "Thumbs.db");
        currentDir = currentDir.toAbsolutePath().normalize();

        Set<Path> normalizedRoots = new HashSet<>();
        for (Path root : libraryRoots) {
            normalizedRoots.add(root.toAbsolutePath().normalize());
        }

        while (currentDir != null) {
            boolean isLibraryRoot = false;
            for (Path root : normalizedRoots) {
                try {
                    if (Files.isSameFile(root, currentDir)) {
                        isLibraryRoot = true;
                        break;
                    }
                } catch (IOException e) {
                    log.warn("Failed to compare paths: {} and {}", root, currentDir);
                }
            }

            if (isLibraryRoot) {
                log.debug("Reached library root: {}. Stopping cleanup.", currentDir);
                break;
            }

            File[] files = currentDir.toFile().listFiles();
            if (files == null) {
                log.warn("Cannot read directory: {}. Stopping cleanup.", currentDir);
                break;
            }

            boolean hasImportantFiles = false;
            for (File file : files) {
                if (!ignoredFilenames.contains(file.getName())) {
                    hasImportantFiles = true;
                    break;
                }
            }

            if (!hasImportantFiles) {
                for (File file : files) {
                    try {
                        Files.delete(file.toPath());
                        log.info("Deleted ignored file: {}", file.getAbsolutePath());
                    } catch (IOException e) {
                        log.warn("Failed to delete ignored file: {}", file.getAbsolutePath());
                    }
                }
                try {
                    Files.delete(currentDir);
                    log.info("Deleted empty directory: {}", currentDir);
                } catch (IOException e) {
                    log.warn("Failed to delete directory: {}", currentDir, e);
                    break;
                }
                currentDir = currentDir.getParent();
            } else {
                log.debug("Directory {} contains important files. Stopping cleanup.", currentDir);
                break;
            }
        }
    }
}