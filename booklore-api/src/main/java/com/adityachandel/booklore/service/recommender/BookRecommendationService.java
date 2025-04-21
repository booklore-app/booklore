package com.adityachandel.booklore.service.recommender;

import com.adityachandel.booklore.exception.ApiError;
import com.adityachandel.booklore.mapper.BookMapper;
import com.adityachandel.booklore.model.dto.Book;
import com.adityachandel.booklore.model.dto.BookRecommendation;
import com.adityachandel.booklore.model.dto.BookRecommendationLite;
import com.adityachandel.booklore.model.entity.AuthorEntity;
import com.adityachandel.booklore.model.entity.BookEntity;
import com.adityachandel.booklore.model.entity.BookMetadataEntity;
import com.adityachandel.booklore.repository.BookRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.AbstractMap.SimpleEntry;
import java.util.stream.Collectors;

@Slf4j
@AllArgsConstructor
@Service
public class BookRecommendationService {

    private final BookSimilarityService similarityService;
    private final BookRepository bookRepository;
    private final BookMapper bookMapper;

    private static final int MAX_BOOKS_PER_AUTHOR = 3;

    public List<BookRecommendation> getRecommendations(Long bookId, int limit) {
        BookEntity book = bookRepository.findById(bookId).orElseThrow(() -> ApiError.BOOK_NOT_FOUND.createException(bookId));
        List<BookRecommendationLite> recommendations = book.getSimilarBooksJson();
        if (recommendations == null || recommendations.isEmpty()) {
            log.info("Recommendations for book ID {} are missing or empty. Computing similarity...", bookId);
            recommendations = findSimilarBookIds(bookId, limit);
            book.setSimilarBooksJson(recommendations);
            bookRepository.save(book);
        }
        List<Long> recommendedBookIds = recommendations.stream()
                .map(BookRecommendationLite::getB)
                .collect(Collectors.toList());

        Map<Long, BookEntity> recommendedBooksMap = bookRepository.findAllById(recommendedBookIds).stream()
                .collect(Collectors.toMap(BookEntity::getId, bookEntity -> bookEntity));

        return recommendations.stream()
                .limit(limit)
                .map(id -> {
                    BookEntity recommendedBook = recommendedBooksMap.get(id.getB());
                    return new BookRecommendation(bookMapper.toBookWithDescription(recommendedBook, false), id.getS());
                })
                .collect(Collectors.toList());
    }

    protected List<BookRecommendationLite> findSimilarBookIds(Long bookId, int limit) {
        List<BookRecommendation> similarBooks = findSimilarBooks(bookId, limit);
        if (similarBooks == null || similarBooks.isEmpty()) {
            return Collections.emptyList();
        }
        return similarBooks.stream()
                .map(b -> new BookRecommendationLite(b.getBook().getId(), b.getSimilarityScore()))
                .collect(Collectors.toList());
    }

    protected List<BookRecommendation> findSimilarBooks(Long bookId, int limit) {
        BookEntity target = bookRepository.findById(bookId).orElseThrow(() -> ApiError.BOOK_NOT_FOUND.createException(bookId));

        List<BookEntity> candidates = bookRepository.findAll();

        String targetSeriesName = Optional.ofNullable(target.getMetadata())
                .map(BookMetadataEntity::getSeriesName)
                .map(String::toLowerCase)
                .orElse(null);

        List<SimpleEntry<BookEntity, Double>> scored = candidates.stream()
                .filter(candidate -> !candidate.getId().equals(bookId))
                .filter(candidate -> {
                    String candidateSeriesName = Optional.ofNullable(candidate.getMetadata())
                            .map(BookMetadataEntity::getSeriesName)
                            .map(String::toLowerCase)
                            .orElse(null);
                    return targetSeriesName == null || !targetSeriesName.equals(candidateSeriesName);
                })
                .map(candidate -> new SimpleEntry<>(candidate, similarityService.calculateSimilarity(target, candidate)))
                .filter(entry -> entry.getValue() > 0.0)
                .sorted(Map.Entry.<BookEntity, Double>comparingByValue().reversed())
                .toList();

        Map<String, Integer> authorCounts = new HashMap<>();
        List<BookRecommendation> recommendations = new ArrayList<>();

        for (SimpleEntry<BookEntity, Double> entry : scored) {
            if (recommendations.size() >= limit) break;
            BookEntity book = entry.getKey();
            Set<String> authorNames = getAuthorNames(book);
            boolean allowed = authorNames.stream()
                    .allMatch(name -> authorCounts.getOrDefault(name, 0) < MAX_BOOKS_PER_AUTHOR);
            if (allowed) {
                Book dto = bookMapper.toBookWithDescription(book, false);
                recommendations.add(new BookRecommendation(dto, entry.getValue()));
                authorNames.forEach(name -> authorCounts.merge(name, 1, Integer::sum));
            }
        }

        return recommendations;
    }

    private Set<String> getAuthorNames(BookEntity book) {
        if (book.getMetadata() == null || book.getMetadata().getAuthors() == null) return Collections.emptySet();
        return book.getMetadata().getAuthors().stream()
                .map(AuthorEntity::getName)
                .filter(Objects::nonNull)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }
}