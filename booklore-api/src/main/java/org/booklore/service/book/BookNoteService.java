package org.booklore.service.book;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.booklore.config.security.service.AuthenticationService;
import org.booklore.exception.ApiError;
import org.booklore.model.dto.BookLoreUser;
import org.booklore.model.dto.BookNote;
import org.booklore.model.dto.CreateBookNoteRequest;
import org.booklore.model.dto.notes.BookNoteFile;
import org.booklore.model.entity.BookEntity;
import org.booklore.repository.BookRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Book notes are stored in a {@code <book name>.notes.json} file in the same folder as the book,
 * not in the database. Each user only ever sees and changes their own notes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookNoteService {

    private final BookRepository bookRepository;
    private final BookNoteFileStore fileStore;
    private final AuthenticationService authenticationService;

    @Transactional(readOnly = true)
    public List<BookNote> getNotesForBook(Long bookId) {
        BookLoreUser user = authenticationService.getAuthenticatedUser();
        Path bookPath = resolveBookPath(bookId);

        BookNoteFile file;
        synchronized (fileStore.lockFor(bookPath)) {
            file = readFile(bookPath);
        }

        return file.getNotes().stream()
                .filter(entry -> isOwnedBy(entry, user))
                .sorted(Comparator.comparing(BookNoteFile.Entry::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(entry -> toDto(entry, bookId, user))
                .toList();
    }

    @Transactional(readOnly = true)
    public BookNote createOrUpdateNote(CreateBookNoteRequest request) {
        BookLoreUser user = authenticationService.getAuthenticatedUser();
        Path bookPath = resolveBookPath(request.getBookId());
        Instant now = Instant.now();

        synchronized (fileStore.lockFor(bookPath)) {
            BookNoteFile file = readFile(bookPath);
            BookNoteFile.Entry entry;

            if (request.getId() != null) {
                entry = file.getNotes().stream()
                        .filter(n -> request.getId().equals(n.getId()) && isOwnedBy(n, user))
                        .findFirst()
                        .orElseThrow(() -> new EntityNotFoundException("Note not found: " + request.getId()));
                entry.setTitle(request.getTitle());
                entry.setContent(request.getContent());
                entry.setUpdatedAt(now);
            } else {
                entry = BookNoteFile.Entry.builder()
                        .id(UUID.randomUUID().toString())
                        .owner(user.getUsername())
                        .title(request.getTitle())
                        .content(request.getContent())
                        .createdAt(now)
                        .updatedAt(now)
                        .build();
                file.getNotes().add(entry);
            }

            writeFile(bookPath, file);
            return toDto(entry, request.getBookId(), user);
        }
    }

    @Transactional(readOnly = true)
    public void deleteNote(Long bookId, String noteId) {
        BookLoreUser user = authenticationService.getAuthenticatedUser();
        Path bookPath = resolveBookPath(bookId);

        synchronized (fileStore.lockFor(bookPath)) {
            BookNoteFile file = readFile(bookPath);
            boolean removed = file.getNotes().removeIf(n -> noteId.equals(n.getId()) && isOwnedBy(n, user));
            if (!removed) {
                throw new EntityNotFoundException("Note not found: " + noteId);
            }
            writeFile(bookPath, file);
        }
    }

    private Path resolveBookPath(Long bookId) {
        BookEntity book = bookRepository.findByIdWithBookFiles(bookId)
                .orElseThrow(() -> ApiError.BOOK_NOT_FOUND.createException(bookId));
        Path bookPath = book.getFullFilePath();
        if (bookPath == null) {
            throw ApiError.FILE_NOT_FOUND.createException("Book " + bookId + " has no file path, so its notes folder is unknown");
        }
        return bookPath;
    }

    private BookNoteFile readFile(Path bookPath) {
        try {
            return fileStore.read(bookPath);
        } catch (IOException e) {
            log.error("Failed to read notes file for {}: {}", bookPath, e.getMessage());
            throw ApiError.INTERNAL_SERVER_ERROR.createException("Could not read the notes file next to this book: " + e.getMessage());
        }
    }

    private void writeFile(Path bookPath, BookNoteFile file) {
        try {
            fileStore.write(bookPath, file);
        } catch (IOException e) {
            log.error("Failed to write notes file for {}: {}", bookPath, e.getMessage());
            throw ApiError.INTERNAL_SERVER_ERROR.createException("Could not save the notes file next to this book (is the folder writable?): " + e.getMessage());
        }
    }

    private boolean isOwnedBy(BookNoteFile.Entry entry, BookLoreUser user) {
        return entry.getOwner() != null && entry.getOwner().equals(user.getUsername());
    }

    private BookNote toDto(BookNoteFile.Entry entry, Long bookId, BookLoreUser user) {
        return BookNote.builder()
                .id(entry.getId())
                .userId(user.getId())
                .bookId(bookId)
                .title(entry.getTitle())
                .content(entry.getContent())
                .createdAt(toLocal(entry.getCreatedAt()))
                .updatedAt(toLocal(entry.getUpdatedAt()))
                .build();
    }

    private LocalDateTime toLocal(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }
}
