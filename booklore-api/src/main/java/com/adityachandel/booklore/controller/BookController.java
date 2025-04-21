package com.adityachandel.booklore.controller;

import com.adityachandel.booklore.model.dto.Book;
import com.adityachandel.booklore.model.dto.BookRecommendation;
import com.adityachandel.booklore.model.dto.BookViewerSettings;
import com.adityachandel.booklore.model.dto.request.ReadProgressRequest;
import com.adityachandel.booklore.model.dto.request.ShelvesAssignmentRequest;
import com.adityachandel.booklore.service.recommender.BookRecommendationService;
import com.adityachandel.booklore.service.BooksService;
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

    private final BooksService booksService;
    private final BookRecommendationService bookRecommendationService;

    @GetMapping
    public ResponseEntity<List<Book>> getBooks(@RequestParam(required = false, defaultValue = "false") boolean withDescription) {
        return ResponseEntity.ok(booksService.getBooks(withDescription));
    }

    @GetMapping("/{bookId}")
    public ResponseEntity<Book> getBook(@PathVariable long bookId, @RequestParam(required = false, defaultValue = "false") boolean withDescription) {
        return ResponseEntity.ok(booksService.getBook(bookId, withDescription));
    }

    @GetMapping("/{bookId}/cover")
    public ResponseEntity<Resource> getBookCover(@PathVariable long bookId) {
        return ResponseEntity.ok(booksService.getBookCover(bookId));
    }

    @GetMapping("/{bookId}/content")
    public ResponseEntity<ByteArrayResource> getBookContent(@PathVariable long bookId) throws IOException {
        return booksService.getBookContent(bookId);
    }

    @GetMapping("/{bookId}/download")
    public ResponseEntity<Resource> downloadBook(@PathVariable("bookId") Long bookId) {
        return booksService.downloadBook(bookId);
    }

    @GetMapping("/{bookId}/viewer-setting")
    public ResponseEntity<BookViewerSettings> getBookViewerSettings(@PathVariable long bookId) {
        return ResponseEntity.ok(booksService.getBookViewerSetting(bookId));
    }

    @PutMapping("/{bookId}/viewer-setting")
    public ResponseEntity<Void> updateBookViewerSettings(@RequestBody BookViewerSettings bookViewerSettings, @PathVariable long bookId) {
        booksService.updateBookViewerSetting(bookId, bookViewerSettings);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/shelves")
    public ResponseEntity<List<Book>> addBookToShelf(@RequestBody @Valid ShelvesAssignmentRequest request) {
        return ResponseEntity.ok(booksService.assignShelvesToBooks(request.getBookIds(), request.getShelvesToAssign(), request.getShelvesToUnassign()));
    }

    @PostMapping("/progress")
    public ResponseEntity<Void> addBookToProgress(@RequestBody @Valid ReadProgressRequest request) {
        booksService.updateReadProgress(request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/recommendations")
    public ResponseEntity<List<BookRecommendation>> getRecommendations(@PathVariable Long id, @RequestParam(defaultValue = "25") @Max(25) @Min(1) int limit) {
        return ResponseEntity.ok(bookRecommendationService.getRecommendations(id, limit));
    }
}