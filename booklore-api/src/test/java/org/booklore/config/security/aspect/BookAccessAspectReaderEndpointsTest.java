package org.booklore.config.security.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.booklore.config.security.service.AuthenticationService;
import org.booklore.controller.CbxReaderController;
import org.booklore.controller.PdfReaderController;
import org.booklore.exception.APIException;
import org.booklore.model.dto.BookLoreUser;
import org.booklore.model.dto.Library;
import org.booklore.model.entity.BookEntity;
import org.booklore.model.entity.LibraryEntity;
import org.booklore.repository.BookRepository;
import org.booklore.service.restriction.ContentRestrictionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * PdfReaderController#listPages/getBookInfo and CbxReaderController#listPages/getPageInfo
 * used to have no {@code @CheckBookAccess}, so any authenticated user could read a book's
 * page count and PDF outline (chapter titles) for any book ID, including books outside
 * their assigned libraries or explicitly content-restricted for them. The image endpoints
 * for the same books (BookMediaController) were already gated by the annotation; these
 * four were not.
 *
 * This drives the real {@link BookAccessAspect} against the actual annotated methods
 * (via reflection, so it fails again if the annotation is ever removed), without needing
 * a Spring context or AOP weaving.
 */
@ExtendWith(MockitoExtension.class)
class BookAccessAspectReaderEndpointsTest {

    private static final long BOOK_ID = 42L;
    private static final long OWN_LIBRARY_ID = 1L;
    private static final long OTHER_LIBRARY_ID = 2L;

    @Mock
    private AuthenticationService authenticationService;
    @Mock
    private BookRepository bookRepository;
    @Mock
    private ContentRestrictionService contentRestrictionService;

    private BookAccessAspect aspect;
    private BookEntity book;

    @BeforeEach
    void setUp() {
        aspect = new BookAccessAspect(authenticationService, bookRepository, contentRestrictionService);
        book = BookEntity.builder()
                .id(BOOK_ID)
                .library(LibraryEntity.builder().id(OWN_LIBRARY_ID).build())
                .build();
        lenient().when(bookRepository.findById(BOOK_ID)).thenReturn(Optional.of(book));
    }

    static Stream<Method> readerEndpoints() throws NoSuchMethodException {
        return Stream.of(
                PdfReaderController.class.getMethod("listPages", Long.class, String.class),
                PdfReaderController.class.getMethod("getBookInfo", Long.class, String.class),
                CbxReaderController.class.getMethod("listPages", Long.class, String.class),
                CbxReaderController.class.getMethod("getPageInfo", Long.class, String.class)
        );
    }

    @ParameterizedTest
    @MethodSource("readerEndpoints")
    void rejectsAUserWithNoAccessToTheBooksLibrary(Method method) {
        when(authenticationService.getAuthenticatedUser()).thenReturn(userWithLibraries(OTHER_LIBRARY_ID));

        APIException exception = assertThrows(APIException.class, () -> aspect.checkBookAccess(joinPointFor(method)));
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
    }

    @ParameterizedTest
    @MethodSource("readerEndpoints")
    void rejectsAUserTheBookIsContentRestrictedFor(Method method) {
        when(authenticationService.getAuthenticatedUser()).thenReturn(userWithLibraries(OWN_LIBRARY_ID));
        when(contentRestrictionService.applyRestrictions(List.of(book), userWithLibraries(OWN_LIBRARY_ID).getId()))
                .thenReturn(List.of());

        APIException exception = assertThrows(APIException.class, () -> aspect.checkBookAccess(joinPointFor(method)));
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
    }

    @ParameterizedTest
    @MethodSource("readerEndpoints")
    void allowsAUserWithLibraryAccessAndNoRestriction(Method method) {
        BookLoreUser user = userWithLibraries(OWN_LIBRARY_ID);
        when(authenticationService.getAuthenticatedUser()).thenReturn(user);
        when(contentRestrictionService.applyRestrictions(List.of(book), user.getId())).thenReturn(List.of(book));

        assertDoesNotThrow(() -> aspect.checkBookAccess(joinPointFor(method)));
    }

    private static BookLoreUser userWithLibraries(long... libraryIds) {
        BookLoreUser.UserPermissions permissions = new BookLoreUser.UserPermissions();
        permissions.setAdmin(false);
        return BookLoreUser.builder()
                .id(7L)
                .permissions(permissions)
                .assignedLibraries(java.util.Arrays.stream(libraryIds)
                        .mapToObj(id -> Library.builder().id(id).build())
                        .toList())
                .build();
    }

    private static JoinPoint joinPointFor(Method method) {
        MethodSignature signature = org.mockito.Mockito.mock(MethodSignature.class);
        lenient().when(signature.getMethod()).thenReturn(method);
        lenient().when(signature.getParameterNames()).thenReturn(new String[]{"bookId", "bookType"});

        JoinPoint joinPoint = org.mockito.Mockito.mock(JoinPoint.class);
        lenient().when(joinPoint.getSignature()).thenReturn(signature);
        lenient().when(joinPoint.getArgs()).thenReturn(new Object[]{BOOK_ID, null});
        return joinPoint;
    }
}
