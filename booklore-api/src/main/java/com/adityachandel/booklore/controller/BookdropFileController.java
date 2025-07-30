package com.adityachandel.booklore.controller;

import com.adityachandel.booklore.mapper.BookdropFileMapper;
import com.adityachandel.booklore.model.dto.BookdropFile;
import com.adityachandel.booklore.model.dto.BookdropFileNotification;
import com.adityachandel.booklore.model.dto.request.BookdropFinalizeRequest;
import com.adityachandel.booklore.model.dto.response.BookdropFinalizeResult;
import com.adityachandel.booklore.model.entity.BookdropFileEntity;
import com.adityachandel.booklore.repository.BookdropFileRepository;
import com.adityachandel.booklore.service.bookdrop.BookDropService;
import lombok.AllArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
@RestController
@RequestMapping("/api/bookdrop")
public class BookdropFileController {

    private final BookdropFileRepository repository;
    private final BookdropFileMapper mapper;
    private final BookDropService bookDropService;

    @GetMapping("/notification")
    public BookdropFileNotification getSummary() {
        long pendingCount = repository.countByStatus(BookdropFileEntity.Status.PENDING_REVIEW);
        long totalCount = repository.count();

        return new BookdropFileNotification(
                (int) pendingCount,
                (int) totalCount,
                Instant.now().toString()
        );
    }

    @GetMapping("/files")
    public List<BookdropFile> getFilesByStatus(@RequestParam(required = false) String status) {
        if ("pending".equalsIgnoreCase(status)) {
            return repository.findAllByStatus(BookdropFileEntity.Status.PENDING_REVIEW)
                    .stream()
                    .map(mapper::toDto)
                    .collect(Collectors.toList());
        }
        return repository.findAll()
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @DeleteMapping("/files")
    public ResponseEntity<Void> discardAllFiles() {
        bookDropService.discardAllFiles();
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
        if (file == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=cover.jpg")
                .contentType(MediaType.IMAGE_JPEG)
                .body(file);
    }
}