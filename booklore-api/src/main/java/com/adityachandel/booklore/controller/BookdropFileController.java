package com.adityachandel.booklore.controller;

import com.adityachandel.booklore.model.dto.BookdropFile;
import com.adityachandel.booklore.model.dto.BookdropFileNotification;
import com.adityachandel.booklore.model.dto.request.BookdropFinalizeRequest;
import com.adityachandel.booklore.model.dto.request.BookdropSelectionRequest;
import com.adityachandel.booklore.model.dto.response.BookdropFinalizeResult;
import com.adityachandel.booklore.service.bookdrop.BookDropService;
import lombok.AllArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@AllArgsConstructor
@RestController
@RequestMapping("/api/v1/bookdrop")
public class BookdropFileController {

    private final BookDropService bookDropService;

    @GetMapping("/notification")
    public BookdropFileNotification getSummary() {
        return bookDropService.getFileNotificationSummary();
    }

    @GetMapping("/files")
    public Page<BookdropFile> getFilesByStatus(@RequestParam(required = false) String status, Pageable pageable) {
        return bookDropService.getFilesByStatus(status, pageable);
    }

    @PostMapping("/files/discard")
    public ResponseEntity<Void> discardSelectedFiles(@RequestBody BookdropSelectionRequest request) {
        bookDropService.discardSelectedFiles(request.isSelectAll(), request.getExcludedIds(), request.getSelectedIds());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/imports/finalize")
    public ResponseEntity<BookdropFinalizeResult> finalizeImport(@RequestBody BookdropFinalizeRequest request) {
        BookdropFinalizeResult result = bookDropService.finalizeImport(request);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{bookdropId}/cover")
    public ResponseEntity<Resource> getBookdropCover(@PathVariable long bookdropId) {
        Resource file = bookDropService.getBookdropCover(bookdropId);
        return (file != null)
                ? ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=cover.jpg")
                .contentType(MediaType.IMAGE_JPEG)
                .body(file)
                : ResponseEntity.noContent().build();
    }
}