package org.booklore.service.book;

import org.booklore.mapper.v2.BookMapperV2;
import org.booklore.model.dto.Book;
import org.booklore.model.dto.BookFile;
import org.booklore.model.dto.BookMetadata;
import org.booklore.model.dto.ComicMetadata;
import org.booklore.model.dto.Shelf;
import org.booklore.model.entity.BookEntity;
import org.booklore.model.enums.BookFileType;
import org.booklore.model.enums.IconType;
import org.booklore.repository.BookRepository;
import org.booklore.service.restriction.ContentRestrictionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class BookQueryService {

    private final BookRepository bookRepository;
    private final BookMapperV2 bookMapperV2;
    private final ContentRestrictionService contentRestrictionService;

    public List<Book> getAllBooks(boolean includeDescription) {
        long t0 = System.nanoTime();
        List<Object[]> rows = bookRepository.findAllBookListRows();
        long t1 = System.nanoTime();

        Map<Long, List<String>> authors = groupStrings(bookRepository.findAllAuthorRows());
        Map<Long, Set<String>> categories = groupStringSet(bookRepository.findAllCategoryRows());
        Map<Long, Set<String>> moods = groupStringSet(bookRepository.findAllMoodRows());
        Map<Long, Set<String>> tags = groupStringSet(bookRepository.findAllTagRows());
        Map<Long, Integer> shelfCounts = new HashMap<>();
        for (Object[] c : bookRepository.findShelfBookCounts()) {
            shelfCounts.put((Long) c[0], ((Number) c[1]).intValue());
        }
        Map<Long, Set<Shelf>> shelves = groupShelves(bookRepository.findAllShelfRows(), shelfCounts);
        Map<Long, List<BookFile>> files = groupFiles(bookRepository.findAllBookFileRows());
        long t2 = System.nanoTime();

        List<Book> result = new ArrayList<>(rows.size());
        for (Object[] r : rows) {
            result.add(assembleBook(r, authors, categories, moods, tags, shelves, files));
        }
        log.info("getAllBooks(projection): {} books | projection {} ms | batches {} ms | assemble {} ms",
                rows.size(), (t1 - t0) / 1_000_000, (t2 - t1) / 1_000_000, (System.nanoTime() - t2) / 1_000_000);
        return result;
    }

    private static <T> Set<T> nullIfEmpty(Set<T> s) { return (s == null || s.isEmpty()) ? null : s; }
    private static <T> List<T> nullIfEmpty(List<T> l) { return (l == null || l.isEmpty()) ? null : l; }

    private Book assembleBook(Object[] r,
                              Map<Long, List<String>> authors,
                              Map<Long, Set<String>> categories,
                              Map<Long, Set<String>> moods,
                              Map<Long, Set<String>> tags,
                              Map<Long, Set<Shelf>> shelves,
                              Map<Long, List<BookFile>> files) {
        Long id = (Long) r[0];
        BookMetadata meta = BookMetadata.builder()
                .bookId(id)
                .title((String) r[6])
                .publisher((String) r[7])
                .publishedDate((java.time.LocalDate) r[8])
                .seriesName((String) r[9])
                .seriesNumber((Float) r[10])
                .isbn13((String) r[11])
                .isbn10((String) r[12])
                .pageCount((Integer) r[13])
                .language((String) r[14])
                .narrator((String) r[15])
                .rating((Double) r[16])
                .amazonRating((Double) r[17])
                .amazonReviewCount((Integer) r[18])
                .goodreadsRating((Double) r[19])
                .goodreadsReviewCount((Integer) r[20])
                .hardcoverRating((Double) r[21])
                .hardcoverReviewCount((Integer) r[22])
                .ranobedbRating((Double) r[23])
                .ageRating((Integer) r[24])
                .contentRating((String) r[25])
                .coverUpdatedOn((Instant) r[26])
                .audiobookCoverUpdatedOn((Instant) r[27])
                .allMetadataLocked(false)
                .authors(nullIfEmpty(authors.get(id)))
                .categories(nullIfEmpty(categories.get(id)))
                .moods(nullIfEmpty(moods.get(id)))
                .tags(nullIfEmpty(tags.get(id)))
                .build();

        List<BookFile> bf = files.getOrDefault(id, List.of());
        BookFile primary = bf.isEmpty() ? null : bf.getFirst();
        List<BookFile> alts = bf.size() > 1 ? bf.subList(1, bf.size()) : null;
        return Book.builder()
                .id(id)
                .metadataMatchScore((Float) r[1])
                .isPhysical((Boolean) r[2])
                .addedOn((Instant) r[3])
                .libraryId((Long) r[4])
                .libraryName((String) r[5])
                .metadata(meta)
                .primaryFile(primary)
                .alternativeFormats(alts)
                .shelves(shelves.getOrDefault(id, Set.of()))
                .build();
    }

    private Map<Long, List<String>> groupStrings(List<Object[]> rows) {
        Map<Long, List<String>> map = new HashMap<>();
        for (Object[] r : rows) {
            map.computeIfAbsent((Long) r[0], k -> new ArrayList<>()).add((String) r[1]);
        }
        return map;
    }

    private Map<Long, Set<String>> groupStringSet(List<Object[]> rows) {
        Map<Long, Set<String>> map = new HashMap<>();
        for (Object[] r : rows) {
            map.computeIfAbsent((Long) r[0], k -> new LinkedHashSet<>()).add((String) r[1]);
        }
        return map;
    }

    private Map<Long, Set<Shelf>> groupShelves(List<Object[]> rows, Map<Long, Integer> counts) {
        Map<Long, Set<Shelf>> map = new HashMap<>();
        for (Object[] r : rows) {
            Long shelfId = (Long) r[1];
            Shelf s = Shelf.builder()
                    .id(shelfId)
                    .name((String) r[2])
                    .icon((String) r[3])
                    .iconType((IconType) r[4])
                    .userId((Long) r[5])
                    .publicShelf(r[6] != null && (Boolean) r[6])
                    .bookCount(counts.getOrDefault(shelfId, 0))
                    .build();
            map.computeIfAbsent((Long) r[0], k -> new LinkedHashSet<>()).add(s);
        }
        return map;
    }

    private Map<Long, List<BookFile>> groupFiles(List<Object[]> rows) {
        Map<Long, List<BookFile>> map = new HashMap<>();
        for (Object[] r : rows) {
            String fileName = (String) r[2];
            String subPath = (String) r[3];
            String libPath = (String) r[7];
            String filePath = (libPath != null && subPath != null && fileName != null)
                    ? java.nio.file.Paths.get(libPath, subPath, fileName).toString() : null;
            BookFile f = BookFile.builder()
                    .bookId((Long) r[0])
                    .id((Long) r[1])
                    .fileName(fileName)
                    .filePath(filePath)
                    .fileSubPath(subPath)
                    .bookType((BookFileType) r[4])
                    .fileSizeKb((Long) r[5])
                    .addedOn((Instant) r[6])
                    .folderBased(r[8] != null && (Boolean) r[8])
                    .isBook(r[9] != null && (Boolean) r[9])
                    .extension(extractExtension(fileName))
                    .build();
            map.computeIfAbsent((Long) r[0], k -> new ArrayList<>()).add(f);
        }
        return map;
    }

    private static String extractExtension(String fileName) {
        if (fileName == null) return null;
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(dot + 1).toLowerCase() : null;
    }

    public List<Book> getAllBooksByLibraryIds(Set<Long> libraryIds, boolean includeDescription, Long userId) {
        List<BookEntity> books = bookRepository.findAllWithMetadataByLibraryIds(libraryIds);
        books = contentRestrictionService.applyRestrictions(books, userId);
        return mapBooksToDto(books, includeDescription, userId, !includeDescription);
    }

    public List<BookEntity> findAllWithMetadataByIds(Set<Long> bookIds) {
        return bookRepository.findAllWithMetadataByIds(bookIds);
    }

    public List<Book> mapEntitiesToDto(List<BookEntity> entities, boolean includeDescription, Long userId) {
        return mapBooksToDto(entities, includeDescription, userId, !includeDescription);
    }

    public List<BookEntity> getAllFullBookEntities() {
        return bookRepository.findAllFullBooks();
    }

    public void saveAll(List<BookEntity> books) {
        bookRepository.saveAll(books);
    }

    private List<Book> mapBooksToDto(List<BookEntity> books, boolean includeDescription, Long userId, boolean stripForListView) {
        long t0 = System.nanoTime();
        List<Book> result = books.stream()
                .map(book -> mapBookToDto(book, includeDescription, userId, stripForListView))
                .collect(Collectors.toList());
        log.info("mapBooksToDto: {} books | seqMap {} ms", books.size(), (System.nanoTime() - t0) / 1_000_000);
        return result;
    }

    private Book mapBookToDto(BookEntity bookEntity, boolean includeDescription, Long userId, boolean stripForListView) {
        Book dto = bookMapperV2.toDTO(bookEntity);

        if (!includeDescription && dto.getMetadata() != null) {
            dto.getMetadata().setDescription(null);
        }

        if (dto.getShelves() != null && userId != null) {
            dto.setShelves(dto.getShelves().stream()
                    .filter(shelf -> userId.equals(shelf.getUserId()))
                    .collect(Collectors.toSet()));
        }

        if (stripForListView) {
            stripFieldsForListView(dto);
        }

        return dto;
    }

    private void stripFieldsForListView(Book dto) {
        dto.setLibraryPath(null);

        BookMetadata m = dto.getMetadata();
        if (m != null) {
            // Compute allMetadataLocked before stripping lock flags
            m.setAllMetadataLocked(computeAllMetadataLocked(m));

            // Strip lock flags
            m.setTitleLocked(null);
            m.setSubtitleLocked(null);
            m.setPublisherLocked(null);
            m.setPublishedDateLocked(null);
            m.setDescriptionLocked(null);
            m.setSeriesNameLocked(null);
            m.setSeriesNumberLocked(null);
            m.setSeriesTotalLocked(null);
            m.setIsbn13Locked(null);
            m.setIsbn10Locked(null);
            m.setAsinLocked(null);
            m.setGoodreadsIdLocked(null);
            m.setComicvineIdLocked(null);
            m.setHardcoverIdLocked(null);
            m.setHardcoverBookIdLocked(null);
            m.setDoubanIdLocked(null);
            m.setGoogleIdLocked(null);
            m.setPageCountLocked(null);
            m.setLanguageLocked(null);
            m.setAmazonRatingLocked(null);
            m.setAmazonReviewCountLocked(null);
            m.setGoodreadsRatingLocked(null);
            m.setGoodreadsReviewCountLocked(null);
            m.setHardcoverRatingLocked(null);
            m.setHardcoverReviewCountLocked(null);
            m.setDoubanRatingLocked(null);
            m.setDoubanReviewCountLocked(null);
            m.setLubimyczytacIdLocked(null);
            m.setLubimyczytacRatingLocked(null);
            m.setRanobedbIdLocked(null);
            m.setRanobedbRatingLocked(null);
            m.setAudibleIdLocked(null);
            m.setAudibleRatingLocked(null);
            m.setAudibleReviewCountLocked(null);
            m.setExternalUrlLocked(null);
            m.setCoverLocked(null);
            m.setAudiobookCoverLocked(null);
            m.setAuthorsLocked(null);
            m.setCategoriesLocked(null);
            m.setMoodsLocked(null);
            m.setTagsLocked(null);
            m.setReviewsLocked(null);
            m.setNarratorLocked(null);
            m.setAbridgedLocked(null);
            m.setAgeRatingLocked(null);
            m.setContentRatingLocked(null);

            // Strip external IDs
            m.setAsin(null);
            m.setGoodreadsId(null);
            m.setComicvineId(null);
            m.setHardcoverId(null);
            m.setHardcoverBookId(null);
            m.setGoogleId(null);
            m.setLubimyczytacId(null);
            m.setRanobedbId(null);
            m.setAudibleId(null);
            m.setDoubanId(null);

            // Strip unused detail fields
            m.setSubtitle(null);
            m.setSeriesTotal(null);
            m.setAbridged(null);
            m.setExternalUrl(null);
            m.setThumbnailUrl(null);
            m.setProvider(null);
            if (m.getAudiobookMetadata() != null) {
                m.getAudiobookMetadata().setChapters(null);
            }
            m.setBookReviews(null);

            // Strip unused ratings
            m.setDoubanRating(null);
            m.setDoubanReviewCount(null);
            m.setAudibleRating(null);
            m.setAudibleReviewCount(null);
            m.setLubimyczytacRating(null);

            // Strip empty metadata collections
            if (m.getMoods() != null && m.getMoods().isEmpty()) m.setMoods(null);
            if (m.getTags() != null && m.getTags().isEmpty()) m.setTags(null);
            if (m.getAuthors() != null && m.getAuthors().isEmpty()) m.setAuthors(null);
            if (m.getCategories() != null && m.getCategories().isEmpty()) m.setCategories(null);

            // Strip ComicMetadata fields
            ComicMetadata cm = m.getComicMetadata();
            if (cm != null) {
                // Strip comic lock flags
                cm.setIssueNumberLocked(null);
                cm.setVolumeNameLocked(null);
                cm.setVolumeNumberLocked(null);
                cm.setStoryArcLocked(null);
                cm.setStoryArcNumberLocked(null);
                cm.setAlternateSeriesLocked(null);
                cm.setAlternateIssueLocked(null);
                cm.setImprintLocked(null);
                cm.setFormatLocked(null);
                cm.setBlackAndWhiteLocked(null);
                cm.setMangaLocked(null);
                cm.setReadingDirectionLocked(null);
                cm.setWebLinkLocked(null);
                cm.setNotesLocked(null);
                cm.setCreatorsLocked(null);
                cm.setPencillersLocked(null);
                cm.setInkersLocked(null);
                cm.setColoristsLocked(null);
                cm.setLetterersLocked(null);
                cm.setCoverArtistsLocked(null);
                cm.setEditorsLocked(null);
                cm.setCharactersLocked(null);
                cm.setTeamsLocked(null);
                cm.setLocationsLocked(null);

                // Strip non-filter detail fields
                cm.setIssueNumber(null);
                cm.setVolumeName(null);
                cm.setVolumeNumber(null);
                cm.setStoryArc(null);
                cm.setStoryArcNumber(null);
                cm.setAlternateSeries(null);
                cm.setAlternateIssue(null);
                cm.setImprint(null);
                cm.setFormat(null);
                cm.setBlackAndWhite(null);
                cm.setManga(null);
                cm.setReadingDirection(null);
                cm.setWebLink(null);
                cm.setNotes(null);
            }
        }

        // Strip empty book-level collections
        if (dto.getAlternativeFormats() != null && dto.getAlternativeFormats().isEmpty()) dto.setAlternativeFormats(null);
        if (dto.getSupplementaryFiles() != null && dto.getSupplementaryFiles().isEmpty()) dto.setSupplementaryFiles(null);
    }

    private boolean computeAllMetadataLocked(BookMetadata m) {
        Boolean[] bookLocks = {
                m.getTitleLocked(), m.getSubtitleLocked(), m.getPublisherLocked(),
                m.getPublishedDateLocked(), m.getDescriptionLocked(), m.getSeriesNameLocked(),
                m.getSeriesNumberLocked(), m.getSeriesTotalLocked(), m.getIsbn13Locked(),
                m.getIsbn10Locked(), m.getAsinLocked(), m.getGoodreadsIdLocked(),
                m.getComicvineIdLocked(), m.getHardcoverIdLocked(), m.getHardcoverBookIdLocked(),
                m.getDoubanIdLocked(), m.getGoogleIdLocked(), m.getPageCountLocked(),
                m.getLanguageLocked(), m.getAmazonRatingLocked(), m.getAmazonReviewCountLocked(),
                m.getGoodreadsRatingLocked(), m.getGoodreadsReviewCountLocked(),
                m.getHardcoverRatingLocked(), m.getHardcoverReviewCountLocked(),
                m.getDoubanRatingLocked(), m.getDoubanReviewCountLocked(),
                m.getLubimyczytacIdLocked(), m.getLubimyczytacRatingLocked(),
                m.getRanobedbIdLocked(), m.getRanobedbRatingLocked(),
                m.getAudibleIdLocked(), m.getAudibleRatingLocked(), m.getAudibleReviewCountLocked(),
                m.getExternalUrlLocked(), m.getCoverLocked(), m.getAudiobookCoverLocked(),
                m.getAuthorsLocked(), m.getCategoriesLocked(), m.getMoodsLocked(),
                m.getTagsLocked(), m.getReviewsLocked(), m.getNarratorLocked(),
                m.getAbridgedLocked(), m.getAgeRatingLocked(), m.getContentRatingLocked()
        };

        boolean hasAnyLock = false;
        for (Boolean lock : bookLocks) {
            if (Boolean.TRUE.equals(lock)) {
                hasAnyLock = true;
            } else {
                return false;
            }
        }
        return hasAnyLock;
    }
}
