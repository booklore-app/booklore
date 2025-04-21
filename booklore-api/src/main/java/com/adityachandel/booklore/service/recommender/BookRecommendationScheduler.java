package com.adityachandel.booklore.service.recommender;

import com.adityachandel.booklore.model.dto.BookRecommendationLite;
import com.adityachandel.booklore.model.entity.BookEntity;
import com.adityachandel.booklore.repository.BookRepository;
import com.adityachandel.booklore.service.AppSettingService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookRecommendationScheduler {

    private final BookRepository bookRepository;
    private final BookRecommendationService recommendationService;
    private final AppSettingService appSettingService;

    private static final int RECOMMENDATION_LIMIT = 25;

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void updateAllSimilarBooks() {
        if (!appSettingService.getAppSettings().isSimilarBookRecommendation()) {
            log.info("Similar book recommendations are disabled. Skipping scheduled task.");
            return;
        }
        long startTime = System.currentTimeMillis();
        log.info("Scheduled task 'updateAllSimilarBooks' started at: {}. Current timestamp: {}", startTime, startTime);

        List<BookEntity> allBooks = bookRepository.findAll();

        for (BookEntity book : allBooks) {
            try {
                List<BookRecommendationLite> recommendations = recommendationService.findSimilarBookIds(book.getId(), RECOMMENDATION_LIMIT);
                book.setSimilarBooksJson(recommendations);
            } catch (Exception e) {
                log.error("Error updating similar books for book ID {}: {}", book.getId(), e.getMessage(), e);
            }
        }
        bookRepository.saveAll(allBooks);

        long endTime = System.currentTimeMillis();
        log.info("Completed scheduled task 'updateAllSimilarBooks' at: {}. Duration: {} ms", endTime, endTime - startTime);
    }
}