package org.booklore.repository;

import org.booklore.model.entity.BookNoteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Only used for the one-time import of notes that were stored in the database before
 * notes moved to files next to the books. See {@code LegacyBookNoteMigration}.
 */
public interface BookNoteRepository extends JpaRepository<BookNoteEntity, Long> {

    List<BookNoteEntity> findByMigratedToFileFalse();
}
