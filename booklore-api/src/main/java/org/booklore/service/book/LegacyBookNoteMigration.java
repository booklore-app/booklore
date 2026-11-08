package org.booklore.service.book;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.booklore.model.dto.notes.BookNoteFile;
import org.booklore.model.entity.BookEntity;
import org.booklore.model.entity.BookNoteEntity;
import org.booklore.repository.BookNoteRepository;
import org.booklore.repository.BookRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * One-time import of notes that used to be stored in the {@code book_notes} table.
 * <p>
 * Each row is written into the notes file next to its book and then flagged as migrated, so the
 * import is safe to repeat: rows whose book folder is not reachable (for example an unmounted drive)
 * are simply retried on the next start, and notes deleted afterwards are never brought back.
 * The database rows are never deleted, so they remain as a backup.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LegacyBookNoteMigration {

    private final BookNoteRepository bookNoteRepository;
    private final BookRepository bookRepository;
    private final BookNoteFileStore fileStore;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void migrate() {
        List<BookNoteEntity> pending = bookNoteRepository.findByMigratedToFileFalse();
        if (pending.isEmpty()) {
            return;
        }

        log.info("Moving {} book note(s) from the database into files next to their books", pending.size());

        Map<Long, List<BookNoteEntity>> byBook = pending.stream()
                .collect(Collectors.groupingBy(BookNoteEntity::getBookId));

        int migrated = 0;
        int skipped = 0;
        for (Map.Entry<Long, List<BookNoteEntity>> group : byBook.entrySet()) {
            try {
                migrated += migrateBook(group.getKey(), group.getValue());
            } catch (Exception e) {
                skipped += group.getValue().size();
                log.warn("Could not move notes of book {} to a file, will retry on next start: {}", group.getKey(), e.getMessage());
            }
        }

        log.info("Book note migration finished: {} moved, {} left for a later start", migrated, skipped);
    }

    private int migrateBook(Long bookId, List<BookNoteEntity> notes) throws Exception {
        BookEntity book = bookRepository.findByIdWithBookFiles(bookId).orElse(null);
        Path bookPath = book == null ? null : book.getFullFilePath();
        if (bookPath == null) {
            throw new IllegalStateException("book has no file path");
        }

        synchronized (fileStore.lockFor(bookPath)) {
            BookNoteFile file = fileStore.read(bookPath);
            Set<String> existingIds = new HashSet<>();
            file.getNotes().forEach(n -> existingIds.add(n.getId()));

            for (BookNoteEntity note : notes) {
                String id = legacyId(note.getId());
                if (existingIds.add(id)) {
                    file.getNotes().add(BookNoteFile.Entry.builder()
                            .id(id)
                            .owner(note.getUser().getUsername())
                            .title(note.getTitle())
                            .content(note.getContent())
                            .createdAt(toInstant(note.getCreatedAt()))
                            .updatedAt(toInstant(note.getUpdatedAt()))
                            .build());
                }
            }

            fileStore.write(bookPath, file);
        }

        notes.forEach(n -> n.setMigratedToFile(true));
        bookNoteRepository.saveAll(notes);
        return notes.size();
    }

    /** Stable id derived from the old row id, so a repeated import never duplicates a note. */
    static String legacyId(Long databaseId) {
        return UUID.nameUUIDFromBytes(("booklore-legacy-book-note-" + databaseId).getBytes(StandardCharsets.UTF_8)).toString();
    }

    private Instant toInstant(LocalDateTime time) {
        return time == null ? null : time.atZone(ZoneId.systemDefault()).toInstant();
    }
}
