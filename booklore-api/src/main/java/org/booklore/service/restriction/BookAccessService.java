package org.booklore.service.restriction;

import org.booklore.config.security.service.AuthenticationService;
import org.booklore.exception.ApiError;
import org.booklore.model.dto.BookLoreUser;
import org.booklore.model.entity.BookEntity;
import org.booklore.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Central place to verify that the currently authenticated user is allowed to
 * access a given book: admins always can; everyone else needs the book's
 * library assigned to them, and the book must not be excluded by their
 * content restrictions.
 * <p>
 * Backs the {@code @CheckBookAccess} aspect for controller methods where the
 * book ID is a direct method parameter (see {@link org.booklore.config.security.aspect.BookAccessAspect}),
 * and is also safe to call directly from service code for endpoints where the
 * book ID is nested inside a request body and the annotation can't reach it.
 */
@Service
@RequiredArgsConstructor
public class BookAccessService {

    private final AuthenticationService authenticationService;
    private final BookRepository bookRepository;
    private final ContentRestrictionService contentRestrictionService;

    public void assertAccess(Long bookId) {
        BookEntity bookEntity = bookRepository.findById(bookId)
                .orElseThrow(() -> ApiError.BOOK_NOT_FOUND.createException(bookId));
        assertAccess(bookEntity);
    }

    public void assertAccess(BookEntity bookEntity) {
        BookLoreUser user = authenticationService.getAuthenticatedUser();

        if (user.getPermissions().isAdmin()) {
            return;
        }

        boolean hasLibraryAccess = user.getAssignedLibraries().stream()
                .anyMatch(library -> library.getId().equals(bookEntity.getLibrary().getId()));

        if (!hasLibraryAccess) {
            throw ApiError.FORBIDDEN.createException("You are not authorized to access this book.");
        }

        List<BookEntity> filteredBooks = contentRestrictionService.applyRestrictions(List.of(bookEntity), user.getId());
        if (filteredBooks.isEmpty()) {
            throw ApiError.FORBIDDEN.createException("You are not authorized to access this book.");
        }
    }
}
