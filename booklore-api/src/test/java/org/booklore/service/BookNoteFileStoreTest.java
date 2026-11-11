package org.booklore.service;

import org.booklore.model.dto.notes.BookNoteFile;
import org.booklore.service.book.BookNoteFileStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class BookNoteFileStoreTest {

    @TempDir
    Path dir;

    private final BookNoteFileStore store = new BookNoteFileStore();

    @Test
    void notesPath_replacesBookExtension() {
        assertEquals(dir.resolve("Dune.notes.json"), BookNoteFileStore.getNotesPath(dir.resolve("Dune.epub")));
        assertEquals(dir.resolve("NoExtension.notes.json"), BookNoteFileStore.getNotesPath(dir.resolve("NoExtension")));
    }

    @Test
    void writeThenRead_roundTrips() throws Exception {
        Path book = dir.resolve("Dune.epub");
        Instant now = Instant.parse("2026-01-02T03:04:05Z");
        BookNoteFile file = new BookNoteFile();
        file.getNotes().add(BookNoteFile.Entry.builder()
                .id("abc").owner("alice").title("T").content("C").createdAt(now).updatedAt(now).build());

        store.write(book, file);
        BookNoteFile read = store.read(book);

        assertEquals(1, read.getNotes().size());
        BookNoteFile.Entry entry = read.getNotes().get(0);
        assertEquals("abc", entry.getId());
        assertEquals("alice", entry.getOwner());
        assertEquals("C", entry.getContent());
        assertEquals(now, entry.getCreatedAt());
    }

    @Test
    void write_leavesNoTempFileBehind() throws Exception {
        Path book = dir.resolve("Dune.epub");
        BookNoteFile file = new BookNoteFile();
        file.getNotes().add(BookNoteFile.Entry.builder().id("a").owner("u").content("c").build());

        store.write(book, file);

        try (var files = Files.list(dir)) {
            assertEquals(1, files.count());
        }
    }

    @Test
    void moveNotesFile_followsBookRename() throws Exception {
        Path oldBook = dir.resolve("Old.epub");
        Path newDir = Files.createDirectory(dir.resolve("sub"));
        Path newBook = newDir.resolve("New.epub");
        Files.writeString(BookNoteFileStore.getNotesPath(oldBook), "{}");

        BookNoteFileStore.moveNotesFile(oldBook, newBook);

        assertFalse(Files.exists(dir.resolve("Old.notes.json")));
        assertTrue(Files.exists(newDir.resolve("New.notes.json")));
    }
}
