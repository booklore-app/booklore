package org.booklore.service.book;

import lombok.extern.slf4j.Slf4j;
import org.booklore.model.dto.notes.BookNoteFile;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reads and writes the per-book notes file that lives next to the book file.
 * <p>
 * Callers that do a read-modify-write cycle must hold {@link #lockFor(Path)} for the
 * whole cycle so concurrent requests cannot overwrite each other.
 */
@Slf4j
@Component
public class BookNoteFileStore {

    public static final String NOTES_SUFFIX = ".notes.json";

    private static final ConcurrentHashMap<Path, Object> LOCKS = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper = JsonMapper.builder()
            .findAndAddModules()
            .configure(SerializationFeature.INDENT_OUTPUT, true)
            .build();

    public static Path getNotesPath(Path bookPath) {
        String fileName = bookPath.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        String baseName = (dotIndex > 0) ? fileName.substring(0, dotIndex) : fileName;
        return bookPath.resolveSibling(baseName + NOTES_SUFFIX);
    }

    public Object lockFor(Path bookPath) {
        return LOCKS.computeIfAbsent(getNotesPath(bookPath).toAbsolutePath().normalize(), p -> new Object());
    }

    /**
     * Returns the notes stored for the book, or an empty file if none exists yet.
     * Throws if the file exists but cannot be parsed, so a damaged file is never silently overwritten.
     */
    public BookNoteFile read(Path bookPath) throws IOException {
        Path notesPath = getNotesPath(bookPath);
        if (!Files.exists(notesPath)) {
            return new BookNoteFile();
        }
        try {
            BookNoteFile file = objectMapper.readValue(Files.readString(notesPath), BookNoteFile.class);
            if (file == null) {
                return new BookNoteFile();
            }
            if (file.getNotes() == null) {
                file.setNotes(new java.util.ArrayList<>());
            }
            return file;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Notes file is not valid JSON: " + notesPath + " (" + e.getMessage() + ")", e);
        }
    }

    /**
     * Writes the notes atomically (temp file + rename). When there are no notes left the file is removed
     * so empty files are not left behind in the library.
     */
    public void write(Path bookPath, BookNoteFile file) throws IOException {
        Path notesPath = getNotesPath(bookPath);

        if (file.getNotes() == null || file.getNotes().isEmpty()) {
            Files.deleteIfExists(notesPath);
            return;
        }

        Path tempPath = notesPath.resolveSibling(notesPath.getFileName() + ".tmp");
        try {
            Files.writeString(tempPath, objectMapper.writeValueAsString(file));
            try {
                Files.move(tempPath, notesPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tempPath, notesPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(tempPath);
        }
    }

    /**
     * Keeps the notes file next to the book when the book file is moved or renamed.
     * Static so it can be called from file-move code without adding dependencies there.
     */
    public static void moveNotesFile(Path oldBookPath, Path newBookPath) {
        if (oldBookPath == null || newBookPath == null) {
            return;
        }
        try {
            Path oldNotes = getNotesPath(oldBookPath);
            Path newNotes = getNotesPath(newBookPath);
            if (oldNotes.equals(newNotes) || !Files.exists(oldNotes)) {
                return;
            }
            Files.createDirectories(newNotes.getParent());
            Files.move(oldNotes, newNotes, StandardCopyOption.REPLACE_EXISTING);
            log.info("Moved notes file from {} to {}", oldNotes, newNotes);
        } catch (IOException e) {
            log.warn("Failed to move notes file from {} to {}: {}", oldBookPath, newBookPath, e.getMessage());
        }
    }
}
