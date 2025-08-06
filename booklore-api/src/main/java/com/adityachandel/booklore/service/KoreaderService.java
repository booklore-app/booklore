package com.adityachandel.booklore.service;

import com.adityachandel.booklore.config.security.KoreaderUserDetails;
import com.adityachandel.booklore.model.dto.progress.KoreaderProgress;
import com.adityachandel.booklore.model.entity.BookEntity;
import com.adityachandel.booklore.model.entity.BookLoreUserEntity;
import com.adityachandel.booklore.model.entity.KoreaderUserEntity;
import com.adityachandel.booklore.model.entity.UserBookProgressEntity;
import com.adityachandel.booklore.model.enums.ReadStatus;
import com.adityachandel.booklore.repository.BookRepository;
import com.adityachandel.booklore.repository.KoreaderUserRepository;
import com.adityachandel.booklore.repository.UserBookProgressRepository;
import com.adityachandel.booklore.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Slf4j
@AllArgsConstructor
@Service
public class KoreaderService {

    private final UserBookProgressRepository progressRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final KoreaderUserRepository koreaderUserRepository;

    public ResponseEntity<Map<String, String>> authorizeUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (!(principal instanceof KoreaderUserDetails koreaderUserDetails)) {
            log.warn("Authorization failed: User not authenticated or invalid principal type");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not authenticated or invalid principal type"));
        }

        Optional<KoreaderUserEntity> userOpt = koreaderUserRepository.findByUsername(koreaderUserDetails.getUsername());
        if (userOpt.isEmpty()) {
            log.warn("Authorization failed: KOReader user '{}' not found", koreaderUserDetails.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not found"));
        }

        KoreaderUserEntity user = userOpt.get();
        String storedPassword = user.getPassword();
        String providedPassword = koreaderUserDetails.getPassword();

        if (storedPassword == null || !storedPassword.equalsIgnoreCase(providedPassword)) {
            log.warn("Authorization failed: Password mismatch for user '{}'", koreaderUserDetails.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid credentials"));
        }

        log.info("KOReader user '{}' authorized successfully", koreaderUserDetails.getUsername());
        return ResponseEntity.ok(Map.of("username", koreaderUserDetails.getUsername()));
    }


    public KoreaderProgress getProgress(String bookHash) {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (!(principal instanceof KoreaderUserDetails koreaderUserDetails)) {
            log.warn("Progress fetch failed: User not authenticated or invalid principal type");
            throw new SecurityException("User not authenticated");
        }

        Long userId = koreaderUserDetails.getBookLoreUserId();

        BookEntity book = bookRepository.findByCurrentHash(bookHash)
                .orElseThrow(() -> new IllegalArgumentException("Book not found for hash " + bookHash));

        UserBookProgressEntity progress = progressRepository.findByUserIdAndBookId(userId, book.getId())
                .orElseThrow(() -> new IllegalArgumentException("No progress found for user and book"));

        return KoreaderProgress.builder()
                .document(bookHash)
                .progress(progress.getKoreaderProgress())
                .percentage(progress.getKoreaderProgressPercent())
                .device("BookLore")
                .device_id("BookLore")
                .build();
    }

    public void saveProgress(String bookHash, KoreaderProgress koreaderProgress, Long userId) {
        BookEntity book = bookRepository.findByCurrentHash(bookHash).orElseThrow(() -> new IllegalArgumentException("Book not found for hash " + bookHash));
        BookLoreUserEntity user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found with id " + userId));

        UserBookProgressEntity progress = progressRepository.findByUserIdAndBookId(userId, book.getId())
                .orElseGet(() -> {
                    UserBookProgressEntity newProgress = new UserBookProgressEntity();
                    newProgress.setUser(user);
                    newProgress.setBook(book);
                    return newProgress;
                });

        progress.setKoreaderProgress(koreaderProgress.getProgress());
        progress.setKoreaderProgressPercent(koreaderProgress.getPercentage());
        progress.setKoreaderDevice(koreaderProgress.getDevice());
        progress.setKoreaderDeviceId(koreaderProgress.getDevice_id());
        progress.setKoreaderLastSyncTime(Instant.now());
        if(koreaderProgress.getPercentage() * 100 >= 0.5) {
            progress.setReadStatus(ReadStatus.READING);
        }
        progress.setLastReadTime(Instant.now());
        progressRepository.save(progress);
    }
}
