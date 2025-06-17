package com.adityachandel.booklore.controller;

import com.adityachandel.booklore.model.dto.Book;
import com.adityachandel.booklore.model.dto.BookRecommendation;
import com.adityachandel.booklore.model.dto.BookViewerSettings;
import com.adityachandel.booklore.model.dto.request.ReadProgressRequest;
import com.adityachandel.booklore.model.dto.request.ShelvesAssignmentRequest;
import com.adityachandel.booklore.service.metadata.MetadataBackupRestoreService;
import com.adityachandel.booklore.service.recommender.BookRecommendationService;
import com.adityachandel.booklore.service.BookService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RequestMapping("/api/v1/books")
@RestController
@AllArgsConstructor
public class BookController {

    private final BookService bookService;
    private final BookRecommendationService bookRecommendationService;
    private final MetadataBackupRestoreService metadataBackupRestoreService;

    @GetMapping
    public ResponseEntity<List<Book>> getBooks(@RequestParam(required = false, defaultValue = "false") boolean withDescription) {
        return ResponseEntity.ok(bookService.getBookDTOs(withDescription));
    }

    @GetMapping("/{bookId}")
    public ResponseEntity<Book> getBook(@PathVariable long bookId, @RequestParam(required = false, defaultValue = "false") boolean withDescription) {
        return ResponseEntity.ok(bookService.getBook(bookId, withDescription));
    }

    @GetMapping("/{bookId}/cover")
    public ResponseEntity<Resource> getBookCover(@PathVariable long bookId) {
        return ResponseEntity.ok(bookService.getBookCover(bookId));
    }

    @GetMapping("/{bookId}/backup-cover")
    public ResponseEntity<Resource> getBackupBookCover(@PathVariable long bookId) {
        Resource file = metadataBackupRestoreService.getBackupCover(bookId);
        return ResponseEntity.ok()
                .header("Content-Disposition", "inline; filename=cover.jpg")
                .contentType(org.springframework.http.MediaType.IMAGE_JPEG)
                .body(file);
    }

    @GetMapping("/{bookId}/content")
    public ResponseEntity<ByteArrayResource> getBookContent(@PathVariable long bookId) throws IOException {
        return bookService.getBookContent(bookId);
    }

    @GetMapping("/{bookId}/download")
    public ResponseEntity<Resource> downloadBook(@PathVariable("bookId") Long bookId) {
        return bookService.downloadBook(bookId);
    }

    @GetMapping("/{bookId}/viewer-setting")
    public ResponseEntity<BookViewerSettings> getBookViewerSettings(@PathVariable long bookId) {
        return ResponseEntity.ok(bookService.getBookViewerSetting(bookId));
    }

    @PutMapping("/{bookId}/viewer-setting")
    public ResponseEntity<Void> updateBookViewerSettings(@RequestBody BookViewerSettings bookViewerSettings, @PathVariable long bookId) {
        bookService.updateBookViewerSetting(bookId, bookViewerSettings);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/shelves")
    public ResponseEntity<List<Book>> addBookToShelf(@RequestBody @Valid ShelvesAssignmentRequest request) {
        return ResponseEntity.ok(bookService.assignShelvesToBooks(request.getBookIds(), request.getShelvesToAssign(), request.getShelvesToUnassign()));
    }

    @PostMapping("/progress")
    public ResponseEntity<Void> addBookToProgress(@RequestBody @Valid ReadProgressRequest request) {
        bookService.updateReadProgress(request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/recommendations")
    public ResponseEntity<List<BookRecommendation>> getRecommendations(@PathVariable Long id, @RequestParam(defaultValue = "25") @Max(25) @Min(1) int limit) {
        return ResponseEntity.ok(bookRecommendationService.getRecommendations(id, limit));
    }
}