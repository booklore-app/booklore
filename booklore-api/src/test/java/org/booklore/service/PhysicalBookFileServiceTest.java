package org.booklore.service;

import org.booklore.config.AppProperties;
import org.booklore.model.entity.AuthorEntity;
import org.booklore.model.entity.BookEntity;
import org.booklore.model.entity.BookFileEntity;
import org.booklore.model.entity.BookMetadataEntity;
import org.booklore.model.entity.LibraryEntity;
import org.booklore.model.entity.LibraryPathEntity;
import org.booklore.model.enums.BookFileType;
import org.booklore.repository.BookAdditionalFileRepository;
import org.booklore.service.book.PhysicalBookFileService;
import org.booklore.service.metadata.sidecar.SidecarMetadataWriter;
import org.booklore.service.monitoring.MonitoringRegistrationService;
import org.booklore.util.FileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PhysicalBookFileServiceTest {

    @TempDir
    Path libraryDir;

    private AppProperties appProperties;
    private MonitoringRegistrationService monitoring;
    private FileService fileService;
    private BookAdditionalFileRepository bookFileRepository;
    private PhysicalBookFileService service;
    private LibraryEntity library;
    private LibraryPathEntity libraryPath;

    @BeforeEach
    void setUp() {
        appProperties = mock(AppProperties.class);
        monitoring = mock(MonitoringRegistrationService.class);
        fileService = mock(FileService.class);
        bookFileRepository = mock(BookAdditionalFileRepository.class);
        SidecarMetadataWriter sidecarWriter = mock(SidecarMetadataWriter.class);

        when(appProperties.isLocalStorage()).thenReturn(true);
        when(fileService.getCoverFile(anyLong())).thenReturn(libraryDir.resolve("no-such-cover.jpg").toString());
        when(bookFileRepository.save(any(BookFileEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(monitoring.isLibraryMonitored(any())).thenReturn(true);
        when(sidecarWriter.isWriteOnScanEnabled()).thenReturn(false);

        libraryPath = LibraryPathEntity.builder().id(1L).path(libraryDir.toString()).build();
        library = LibraryEntity.builder().id(5L).name("Physical").libraryPaths(new ArrayList<>(List.of(libraryPath))).build();

        service = new PhysicalBookFileService(appProperties, monitoring, fileService, bookFileRepository, sidecarWriter);
    }

    private BookEntity physicalBook(String title) {
        BookEntity book = BookEntity.builder().id(10L).library(library).isPhysical(true).build();
        BookMetadataEntity metadata = BookMetadataEntity.builder()
                .title(title)
                .authors(new ArrayList<>(List.of(AuthorEntity.builder().name("Frank Herbert").build())))
                .isbn13("9780441172719")
                .build();
        book.setMetadata(metadata);
        return book;
    }

    @Test
    void createsFileAndAttachesFileRecord() throws Exception {
        BookEntity book = physicalBook("Dune");

        assertTrue(service.attachPlaceholderFile(book));

        Path expected = libraryDir.resolve("Frank Herbert").resolve("Dune").resolve("Dune.epub");
        assertTrue(Files.exists(expected));
        assertEquals(1, book.getBookFiles().size());

        BookFileEntity file = book.getBookFiles().getFirst();
        assertEquals("Dune.epub", file.getFileName());
        assertEquals(Path.of("Frank Herbert", "Dune").toString(), file.getFileSubPath());
        assertEquals(BookFileType.EPUB, file.getBookType());
        assertTrue(file.isBookFormat());
        assertTrue(PhysicalBookFileService.isPlaceholder(file));
        assertNotNull(file.getCurrentHash());
        assertSame(libraryPath, book.getLibraryPath());
        assertEquals(expected, book.getFullFilePath());
        assertTrue(Boolean.TRUE.equals(book.getIsPhysical()), "book must stay flagged as physical");

        try (ZipFile zip = new ZipFile(expected.toFile())) {
            assertNotNull(zip.getEntry("OEBPS/content.opf"));
        }

        verify(monitoring).unregisterLibrary(5L);
        verify(monitoring).registerLibraryPaths(eq(5L), any(Path.class));
    }

    @Test
    void sameTitleGetsNumberedFileName() {
        BookEntity first = physicalBook("Dune");
        BookEntity second = physicalBook("Dune");

        assertTrue(service.attachPlaceholderFile(first));
        assertTrue(service.attachPlaceholderFile(second));

        assertEquals("Dune.epub", first.getBookFiles().getFirst().getFileName());
        assertEquals("Dune (2).epub", second.getBookFiles().getFirst().getFileName());
        assertTrue(Files.exists(libraryDir.resolve("Frank Herbert").resolve("Dune").resolve("Dune (2).epub")));
    }

    @Test
    void unsafeCharactersInTitleAreReplaced() {
        BookEntity book = physicalBook("AC/DC: Live?");

        assertTrue(service.attachPlaceholderFile(book));

        // the library's path resolver drops characters that are not allowed in file names
        assertEquals("ACDC Live.epub", book.getBookFiles().getFirst().getFileName());
        assertEquals(Path.of("Frank Herbert", "ACDC Live").toString(), book.getBookFiles().getFirst().getFileSubPath());
    }

    @Test
    void coverIsEmbeddedWhenOneExists() throws Exception {
        Path cover = libraryDir.resolve("cover-source.jpg");
        Files.write(cover, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 9, 9});
        when(fileService.getCoverFile(anyLong())).thenReturn(cover.toString());
        BookEntity book = physicalBook("Dune");

        assertTrue(service.attachPlaceholderFile(book));

        try (ZipFile zip = new ZipFile(libraryDir.resolve("Frank Herbert").resolve("Dune").resolve("Dune.epub").toFile())) {
            assertNotNull(zip.getEntry("OEBPS/cover.jpg"));
        }
    }

    @Test
    void doesNothingWhenNotOnLocalStorage() {
        when(appProperties.isLocalStorage()).thenReturn(false);
        BookEntity book = physicalBook("Dune");

        assertFalse(service.attachPlaceholderFile(book));

        assertFalse(book.hasFiles());
        assertFalse(Files.exists(libraryDir.resolve("Frank Herbert")));
    }

    @Test
    void doesNothingWhenBookAlreadyHasAFile() {
        BookEntity book = physicalBook("Dune");
        book.getBookFiles().add(BookFileEntity.builder().book(book).fileName("real.epub").fileSubPath("").build());

        assertFalse(service.attachPlaceholderFile(book));

        assertEquals(1, book.getBookFiles().size());
        assertFalse(Files.exists(libraryDir.resolve("Frank Herbert")));
    }

    @Test
    void bookWithoutAuthorIsFiledUnderUnknownAuthor() {
        BookEntity book = physicalBook("Dune");
        book.getMetadata().setAuthors(new ArrayList<>());

        assertTrue(service.attachPlaceholderFile(book));

        assertTrue(Files.exists(libraryDir.resolve("Unknown Author").resolve("Dune").resolve("Dune.epub")));
        assertEquals(Path.of("Unknown Author", "Dune").toString(), book.getBookFiles().getFirst().getFileSubPath());
    }

    @Test
    void placeholderIsHiddenOnlyForBooksFlaggedAsPhysical() {
        BookEntity book = physicalBook("Dune");
        assertTrue(service.attachPlaceholderFile(book));
        BookFileEntity placeholder = book.getBookFiles().getFirst();

        assertTrue(PhysicalBookFileService.isHiddenPlaceholder(placeholder));

        book.setIsPhysical(false);
        assertFalse(PhysicalBookFileService.isHiddenPlaceholder(placeholder),
                "once a book is no longer physical, its file is an ordinary EPUB");
        assertFalse(PhysicalBookFileService.isHiddenPlaceholder(null));

        BookFileEntity realFile = BookFileEntity.builder().book(physicalBook("Other")).fileName("real.epub").build();
        assertFalse(PhysicalBookFileService.isHiddenPlaceholder(realFile));
    }
}
