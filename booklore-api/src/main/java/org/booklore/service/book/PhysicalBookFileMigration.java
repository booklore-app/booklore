package org.booklore.service.book;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.booklore.repository.BookRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

/**
 * One-time catch-up for physical books that were created before they got a file: each one gets its
 * placeholder file in its library folder. Runs before the notes migration (order 1 vs 2), because notes
 * are stored next to a book's file and physical books had none.
 * <p>
 * Safe to repeat: only physical books that still have no file are touched, and each book is handled in its
 * own transaction so one failure (for example a read-only folder) does not affect the others.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PhysicalBookFileMigration {

    private final BookRepository bookRepository;
    private final PhysicalBookFileService physicalBookFileService;
    private final TransactionTemplate transactionTemplate;

    @EventListener(ApplicationReadyEvent.class)
    @Order(1)
    public void migrate() {
        List<Long> bookIds = bookRepository.findPhysicalBookIdsWithoutFiles();
        if (bookIds.isEmpty()) {
            return;
        }

        log.info("Creating files for {} physical book(s) that only existed in the database", bookIds.size());

        int created = 0;
        int failed = 0;
        for (Long bookId : bookIds) {
            try {
                Boolean done = transactionTemplate.execute(status ->
                        bookRepository.findById(bookId)
                                .map(book -> {
                                    boolean attached = physicalBookFileService.attachPlaceholderFile(book);
                                    if (attached) {
                                        bookRepository.save(book);
                                    }
                                    return attached;
                                })
                                .orElse(false));
                if (Boolean.TRUE.equals(done)) {
                    created++;
                }
            } catch (Exception e) {
                failed++;
                log.warn("Could not create a file for physical book {}, will retry on next start: {}", bookId, e.getMessage());
            }
        }

        log.info("Physical book file migration finished: {} created, {} failed", created, failed);
    }
}
