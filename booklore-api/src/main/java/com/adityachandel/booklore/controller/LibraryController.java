package com.adityachandel.booklore.controller;

import com.adityachandel.booklore.config.security.annotation.CheckLibraryAccess;
import com.adityachandel.booklore.model.dto.Book;
import com.adityachandel.booklore.model.dto.Library;
import com.adityachandel.booklore.model.dto.request.CreateLibraryRequest;
import com.adityachandel.booklore.service.library.LibraryService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/libraries")
@AllArgsConstructor
public class LibraryController {

    private final LibraryService libraryService;

    @GetMapping
    public ResponseEntity<List<Library>> getLibraries() {
        return ResponseEntity.ok(libraryService.getLibraries());
    }

    @GetMapping("/{libraryId}")
    @CheckLibraryAccess(libraryIdParam = "libraryId")
    public ResponseEntity<Library> getLibrary(@PathVariable long libraryId) {
        return ResponseEntity.ok(libraryService.getLibrary(libraryId));
    }

    @PostMapping
    @PreAuthorize("@securityUtil.canManipulateLibrary() or @securityUtil.isAdmin()")
    public ResponseEntity<Library> createLibrary(@RequestBody CreateLibraryRequest request) {
        return ResponseEntity.ok(libraryService.createLibrary(request));
    }

    @PutMapping("/{libraryId}")
    @CheckLibraryAccess(libraryIdParam = "libraryId")
    @PreAuthorize("@securityUtil.canManipulateLibrary() or @securityUtil.isAdmin()")
    public ResponseEntity<Library> updateLibrary(@RequestBody CreateLibraryRequest request, @PathVariable Long libraryId) {
        return ResponseEntity.ok(libraryService.updateLibrary(request, libraryId));
    }

    @DeleteMapping("/{libraryId}")
    @CheckLibraryAccess(libraryIdParam = "libraryId")
    @PreAuthorize("@securityUtil.canManipulateLibrary() or @securityUtil.isAdmin()")
    public ResponseEntity<?> deleteLibrary(@PathVariable long libraryId) {
        libraryService.deleteLibrary(libraryId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{libraryId}/book/{bookId}")
    @CheckLibraryAccess(libraryIdParam = "libraryId")
    public ResponseEntity<Book> getBook(@PathVariable long libraryId, @PathVariable long bookId) {
        return ResponseEntity.ok(libraryService.getBook(libraryId, bookId));
    }


    @GetMapping("/{libraryId}/book")
    @CheckLibraryAccess(libraryIdParam = "libraryId")
    public ResponseEntity<List<Book>> getBooks(@PathVariable long libraryId) {
        List<Book> books = libraryService.getBooks(libraryId);
        return ResponseEntity.ok(books);
    }

    @PutMapping("/{libraryId}/refresh")
    @CheckLibraryAccess(libraryIdParam = "libraryId")
    @PreAuthorize("@securityUtil.canManipulateLibrary() or @securityUtil.isAdmin()")
    public ResponseEntity<?> rescanLibrary(@PathVariable long libraryId) {
        libraryService.rescanLibrary(libraryId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{libraryId}/file-naming-pattern")
    @CheckLibraryAccess(libraryIdParam = "libraryId")
    @PreAuthorize("@securityUtil.canManipulateLibrary() or @securityUtil.isAdmin()")
    public ResponseEntity<Library> setFileNamingPattern(@PathVariable long libraryId, @RequestBody Map<String, String> body) {
        String pattern = body.get("fileNamingPattern");
        Library updated = libraryService.setFileNamingPattern(libraryId, pattern);
        return ResponseEntity.ok(updated);
    }
}