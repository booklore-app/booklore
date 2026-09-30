package org.booklore.service;

import org.booklore.model.entity.BookEntity;
import org.booklore.repository.BookRepository;
import org.booklore.service.book.PhysicalBookFileMigration;
import org.booklore.service.book.PhysicalBookFileService;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PhysicalBookFileMigrationTest {

    private final BookRepository bookRepository = mock(BookRepository.class);
    private final PhysicalBookFileService fileService = mock(PhysicalBookFileService.class);
    private final TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
    private final PhysicalBookFileMigration migration = new PhysicalBookFileMigration(bookRepository, fileService, transactionTemplate);

    @SuppressWarnings("unchecked")
    private void runCallbacksImmediately() {
        when(transactionTemplate.execute(any())).thenAnswer(invocation ->
                ((TransactionCallback<Object>) invocation.getArgument(0)).doInTransaction(null));
    }

    @Test
    void createsFilesForPhysicalBooksThatHaveNone() {
        runCallbacksImmediately();
        BookEntity book = BookEntity.builder().id(7L).isPhysical(true).build();
        when(bookRepository.findPhysicalBookIdsWithoutFiles()).thenReturn(List.of(7L));
        when(bookRepository.findById(7L)).thenReturn(Optional.of(book));
        when(fileService.attachPlaceholderFile(book)).thenReturn(true);

        migration.migrate();

        verify(fileService).attachPlaceholderFile(book);
        verify(bookRepository).save(book);
    }

    @Test
    void oneFailureDoesNotStopTheOthers() {
        runCallbacksImmediately();
        BookEntity broken = BookEntity.builder().id(1L).isPhysical(true).build();
        BookEntity fine = BookEntity.builder().id(2L).isPhysical(true).build();
        when(bookRepository.findPhysicalBookIdsWithoutFiles()).thenReturn(List.of(1L, 2L));
        when(bookRepository.findById(1L)).thenReturn(Optional.of(broken));
        when(bookRepository.findById(2L)).thenReturn(Optional.of(fine));
        when(fileService.attachPlaceholderFile(broken)).thenThrow(new IllegalStateException("read-only folder"));
        when(fileService.attachPlaceholderFile(fine)).thenReturn(true);

        migration.migrate();

        verify(bookRepository, never()).save(broken);
        verify(bookRepository).save(fine);
    }

    @Test
    void doesNothingWhenEveryPhysicalBookAlreadyHasAFile() {
        when(bookRepository.findPhysicalBookIdsWithoutFiles()).thenReturn(List.of());

        migration.migrate();

        verifyNoInteractions(fileService);
        verifyNoInteractions(transactionTemplate);
    }
}
