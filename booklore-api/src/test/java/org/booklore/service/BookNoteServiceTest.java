package org.booklore.service;

import jakarta.persistence.EntityNotFoundException;
import org.booklore.config.security.service.AuthenticationService;
import org.booklore.model.dto.BookLoreUser;
import org.booklore.model.dto.BookNote;
import org.booklore.model.dto.CreateBookNoteRequest;
import org.booklore.model.entity.BookEntity;
import org.booklore.repository.BookRepository;
import org.booklore.service.book.BookNoteFileStore;
import org.booklore.service.book.BookNoteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BookNoteServiceTest {

    @TempDir
    Path libraryDir;

    private AuthenticationService authenticationService;
    private BookNoteService service;
    private Path bookPath;

    private final Long bookId = 2L;

    @BeforeEach
    void setUp() throws Exception {
        BookRepository bookRepository = mock(BookRepository.class);
        authenticationService = mock(AuthenticationService.class);
        service = new BookNoteService(bookRepository, new BookNoteFileStore(), authenticationService);

        bookPath = libraryDir.resolve("My Book.epub");
        Files.createFile(bookPath);

        BookEntity book = mock(BookEntity.class);
        when(book.getFullFilePath()).thenReturn(bookPath);
        when(bookRepository.findByIdWithBookFiles(bookId)).thenReturn(Optional.of(book));

        loginAs(1L, "alice");
    }

    private void loginAs(Long id, String username) {
        BookLoreUser user = new BookLoreUser();
        user.setId(id);
        user.setUsername(username);
        when(authenticationService.getAuthenticatedUser()).thenReturn(user);
    }

    private CreateBookNoteRequest request(String id, String title, String content) {
        return CreateBookNoteRequest.builder().id(id).bookId(bookId).title(title).content(content).build();
    }

    @Test
    void getNotesForBook_returnsEmptyListWhenNoFileExists() {
        assertTrue(service.getNotesForBook(bookId).isEmpty());
        assertFalse(Files.exists(libraryDir.resolve("My Book.notes.json")));
    }

    @Test
    void createNote_writesNotesFileNextToBook() {
        BookNote created = service.createOrUpdateNote(request(null, "Title", "Content"));

        assertNotNull(created.getId());
        assertEquals("Title", created.getTitle());
        assertEquals(1L, created.getUserId());
        assertTrue(Files.exists(libraryDir.resolve("My Book.notes.json")));
        assertEquals(1, service.getNotesForBook(bookId).size());
    }

    @Test
    void updateNote_changesContentInPlace() {
        BookNote created = service.createOrUpdateNote(request(null, "Old", "Old content"));

        BookNote updated = service.createOrUpdateNote(request(created.getId(), "New", "New content"));

        assertEquals(created.getId(), updated.getId());
        List<BookNote> notes = service.getNotesForBook(bookId);
        assertEquals(1, notes.size());
        assertEquals("New", notes.get(0).getTitle());
        assertEquals("New content", notes.get(0).getContent());
    }

    @Test
    void updateNote_throwsWhenNoteDoesNotExist() {
        assertThrows(EntityNotFoundException.class,
                () -> service.createOrUpdateNote(request("does-not-exist", "T", "C")));
    }

    @Test
    void notes_areIsolatedBetweenUsers() {
        BookNote aliceNote = service.createOrUpdateNote(request(null, "Alice", "private"));

        loginAs(2L, "bob");
        assertTrue(service.getNotesForBook(bookId).isEmpty());
        assertThrows(EntityNotFoundException.class,
                () -> service.createOrUpdateNote(request(aliceNote.getId(), "Hijack", "x")));
        assertThrows(EntityNotFoundException.class, () -> service.deleteNote(bookId, aliceNote.getId()));

        service.createOrUpdateNote(request(null, "Bob", "mine"));
        assertEquals(1, service.getNotesForBook(bookId).size());

        loginAs(1L, "alice");
        assertEquals(1, service.getNotesForBook(bookId).size());
        assertEquals("Alice", service.getNotesForBook(bookId).get(0).getTitle());
    }

    @Test
    void deleteNote_removesNoteAndDeletesFileWhenEmpty() {
        BookNote created = service.createOrUpdateNote(request(null, "T", "C"));

        service.deleteNote(bookId, created.getId());

        assertTrue(service.getNotesForBook(bookId).isEmpty());
        assertFalse(Files.exists(libraryDir.resolve("My Book.notes.json")));
    }

    @Test
    void corruptNotesFile_isNeverOverwritten() throws Exception {
        Path notesFile = libraryDir.resolve("My Book.notes.json");
        Files.writeString(notesFile, "{ this is not json");

        assertThrows(RuntimeException.class, () -> service.getNotesForBook(bookId));
        assertThrows(RuntimeException.class, () -> service.createOrUpdateNote(request(null, "T", "C")));
        assertEquals("{ this is not json", Files.readString(notesFile));
    }
}
