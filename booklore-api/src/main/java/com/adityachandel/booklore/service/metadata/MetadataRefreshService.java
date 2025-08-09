package com.adityachandel.booklore.service.metadata;

import com.adityachandel.booklore.exception.ApiError;
import com.adityachandel.booklore.mapper.BookMapper;
import com.adityachandel.booklore.model.MetadataUpdateWrapper;
import com.adityachandel.booklore.model.dto.Book;
import com.adityachandel.booklore.model.dto.BookMetadata;
import com.adityachandel.booklore.model.dto.MetadataBatchProgressNotification;
import com.adityachandel.booklore.model.dto.request.FetchMetadataRequest;
import com.adityachandel.booklore.model.dto.request.MetadataRefreshOptions;
import com.adityachandel.booklore.model.dto.request.MetadataRefreshRequest;
import com.adityachandel.booklore.model.dto.settings.AppSettings;
import com.adityachandel.booklore.model.entity.BookEntity;
import com.adityachandel.booklore.model.entity.LibraryEntity;
import com.adityachandel.booklore.model.entity.MetadataFetchJobEntity;
import com.adityachandel.booklore.model.entity.MetadataFetchProposalEntity;
import com.adityachandel.booklore.model.enums.FetchedMetadataProposalStatus;
import com.adityachandel.booklore.model.enums.MetadataFetchTaskStatus;
import com.adityachandel.booklore.model.enums.MetadataProvider;
import com.adityachandel.booklore.model.websocket.Topic;
import com.adityachandel.booklore.repository.BookRepository;
import com.adityachandel.booklore.repository.LibraryRepository;
import com.adityachandel.booklore.repository.MetadataFetchJobRepository;
import com.adityachandel.booklore.repository.MetadataFetchProposalRepository;
import com.adityachandel.booklore.service.BookQueryService;
import com.adityachandel.booklore.service.NotificationService;
import com.adityachandel.booklore.service.appsettings.AppSettingService;
import com.adityachandel.booklore.service.metadata.parser.BookParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static com.adityachandel.booklore.model.enums.MetadataProvider.*;
import static com.adityachandel.booklore.model.websocket.LogNotification.createLogNotification;

@Slf4j
@AllArgsConstructor
@Service
public class MetadataRefreshService {

    private final LibraryRepository libraryRepository;
    private final MetadataFetchJobRepository metadataFetchJobRepository;
    private final MetadataFetchProposalRepository metadataFetchProposalRepository;
    private final BookMapper bookMapper;
    private final BookMetadataUpdater bookMetadataUpdater;
    private final NotificationService notificationService;
    private final AppSettingService appSettingService;
    private final Map<MetadataProvider, BookParser> parserMap;
    private final ObjectMapper objectMapper;
    private final BookRepository bookRepository;
    private final PlatformTransactionManager transactionManager;


    public void refreshMetadata(MetadataRefreshRequest request, Long userId) {
        log.info("Refresh Metadata task started!");

        if (Boolean.TRUE.equals(request.getQuick())) {
            AppSettings appSettings = appSettingService.getAppSettings();
            request.setRefreshOptions(appSettings.getMetadataRefreshOptions());
        }

        List<MetadataProvider> providers = prepareProviders(request);
        Set<Long> bookIds = getBookEntities(request);

        boolean isReviewMode = Boolean.TRUE.equals(request.getRefreshOptions().getReviewBeforeApply());
        MetadataFetchJobEntity task;
        String taskId;

        if (isReviewMode) {
            taskId = UUID.randomUUID().toString();
            task = MetadataFetchJobEntity.builder()
                    .taskId(taskId)
                    .userId(userId)
                    .status(MetadataFetchTaskStatus.IN_PROGRESS)
                    .startedAt(Instant.now())
                    .totalBooksCount(bookIds.size())
                    .completedBooks(0)
                    .build();
            metadataFetchJobRepository.save(task);
        } else {
            taskId = null;
            task = null;
        }

        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        int completedCount = 0;
        try {
            for (Long bookId : bookIds) {
                int finalCompletedCount = completedCount;
                txTemplate.execute(status -> {
                    BookEntity book = bookRepository.findAllWithMetadataByIds(Collections.singleton(bookId))
                            .stream().findFirst()
                            .orElseThrow(() -> ApiError.BOOK_NOT_FOUND.createException(bookId));
                    try {
                        if (book.getMetadata().areAllFieldsLocked()) {
                            log.info("Skipping locked book: {}", book.getFileName());
                            notificationService.sendMessage(Topic.LOG,
                                createLogNotification("Book '" + book.getMetadata().getTitle() + "' is locked."));
                            return null;
                        }
                        reportProgressIfNeeded(task, taskId, finalCompletedCount, bookIds.size(), book);
                        Map<MetadataProvider, BookMetadata> metadataMap = fetchMetadataForBook(providers, book);
                        if (providers.contains(GoodReads)) Thread.sleep(ThreadLocalRandom.current().nextLong(500, 1500));
                        BookMetadata fetched = buildFetchMetadata(book.getId(), request, metadataMap);
                        if (isReviewMode) saveProposal(taskId, book.getId(), fetched);
                        else updateBookMetadata(book, fetched, request.getRefreshOptions().isRefreshCovers(),
                            request.getRefreshOptions().isMergeCategories());
                    } catch (Exception e) {
                        log.error("Metadata update failed for book: {}", book.getFileName(), e);
                    }
                    bookRepository.saveAndFlush(book);
                    return null;
                });
                completedCount++;
            }
            if (isReviewMode) completeTask(task, completedCount, bookIds.size());
        } catch (Exception fatal) {
            log.error("Fatal error during metadata refresh", fatal);
            if (isReviewMode) failTask(task, bookIds.size(), fatal.getMessage());
            throw fatal;
        }
        log.info("Metadata refresh task completed!");
    }

    private void reportProgressIfNeeded(MetadataFetchJobEntity task, String taskId, int completedCount, int total, BookEntity book) {
        if (task == null) return;

        task.setCompletedBooks(completedCount);
        metadataFetchJobRepository.save(task);

        String message = String.format("Processing '%s'", book.getMetadata().getTitle());

        notificationService.sendMessage(Topic.BOOK_METADATA_BATCH_PROGRESS,
                new MetadataBatchProgressNotification(
                        taskId, completedCount, total, message, MetadataFetchTaskStatus.IN_PROGRESS.name()
                ));
    }

    private void completeTask(MetadataFetchJobEntity task, int completed, int total) {
        task.setStatus(MetadataFetchTaskStatus.COMPLETED);
        task.setCompletedAt(Instant.now());
        task.setCompletedBooks(completed);
        metadataFetchJobRepository.save(task);

        notificationService.sendMessage(Topic.BOOK_METADATA_BATCH_PROGRESS,
                new MetadataBatchProgressNotification(
                        task.getTaskId(), completed, total, "Metadata batch update completed",
                        MetadataFetchTaskStatus.COMPLETED.name()
                ));
    }

    private void failTask(MetadataFetchJobEntity task, int total, String errorMessage) {
        task.setStatus(MetadataFetchTaskStatus.ERROR);
        task.setCompletedAt(Instant.now());
        metadataFetchJobRepository.save(task);

        notificationService.sendMessage(Topic.BOOK_METADATA_BATCH_PROGRESS,
                new MetadataBatchProgressNotification(
                        task.getTaskId(), 0, total, "Error: " + errorMessage,
                        MetadataFetchTaskStatus.ERROR.name()
                ));
    }

    private void saveProposal(String taskId, Long bookId, BookMetadata metadata) throws JsonProcessingException {
        MetadataFetchJobEntity job = metadataFetchJobRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        MetadataFetchProposalEntity proposal = MetadataFetchProposalEntity.builder()
                .job(job)
                .bookId(bookId)
                .metadataJson(objectMapper.writeValueAsString(metadata))
                .status(FetchedMetadataProposalStatus.FETCHED)
                .fetchedAt(Instant.now())
                .build();

        metadataFetchProposalRepository.save(proposal);
    }


    public void updateBookMetadata(BookEntity bookEntity, BookMetadata metadata, boolean replaceCover, boolean mergeCategories) {
        if (metadata != null) {
            MetadataUpdateWrapper metadataUpdateWrapper = MetadataUpdateWrapper.builder()
                    .metadata(metadata)
                    .build();
            bookMetadataUpdater.setBookMetadata(bookEntity, metadataUpdateWrapper, replaceCover, mergeCategories);

            Book book = bookMapper.toBook(bookEntity);
            notificationService.sendMessage(Topic.BOOK_METADATA_UPDATE, book);
            notificationService.sendMessage(Topic.LOG, createLogNotification("Book metadata updated: " + book.getMetadata().getTitle()));
        }
    }

    public List<MetadataProvider> prepareProviders(MetadataRefreshRequest request) {
        Set<MetadataProvider> allProviders = new HashSet<>(getAllProvidersUsingIndividualFields(request));
        return new ArrayList<>(allProviders);
    }

    protected Set<MetadataProvider> getAllProvidersUsingIndividualFields(MetadataRefreshRequest request) {
        MetadataRefreshOptions.FieldOptions fieldOptions = request.getRefreshOptions().getFieldOptions();
        Set<MetadataProvider> uniqueProviders = new HashSet<>();

        if (fieldOptions != null) {
            addProviderToSet(fieldOptions.getTitle(), uniqueProviders);
            addProviderToSet(fieldOptions.getDescription(), uniqueProviders);
            addProviderToSet(fieldOptions.getAuthors(), uniqueProviders);
            addProviderToSet(fieldOptions.getCategories(), uniqueProviders);
            addProviderToSet(fieldOptions.getCover(), uniqueProviders);
        }

        return uniqueProviders;
    }

    protected void addProviderToSet(MetadataRefreshOptions.FieldProvider fieldProvider, Set<MetadataProvider> providerSet) {
        if (fieldProvider != null) {
            if (fieldProvider.getP4() != null) providerSet.add(fieldProvider.getP4());
            if (fieldProvider.getP3() != null) providerSet.add(fieldProvider.getP3());
            if (fieldProvider.getP2() != null) providerSet.add(fieldProvider.getP2());
            if (fieldProvider.getP1() != null) providerSet.add(fieldProvider.getP1());
        }
    }

    public Map<MetadataProvider, BookMetadata> fetchMetadataForBook(List<MetadataProvider> providers, Book book) {
        return providers.stream()
                .map(provider -> CompletableFuture.supplyAsync(() -> fetchTopMetadataFromAProvider(provider, book))
                        .exceptionally(e -> {
                            log.error("Error fetching metadata from provider: {}", provider, e);
                            return null;
                        }))
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        BookMetadata::getProvider,
                        metadata -> metadata,
                        (existing, replacement) -> existing
                ));
    }

    protected Map<MetadataProvider, BookMetadata> fetchMetadataForBook(List<MetadataProvider> providers, BookEntity bookEntity) {
        return providers.stream()
                .map(provider -> CompletableFuture.supplyAsync(() -> fetchTopMetadataFromAProvider(provider, bookMapper.toBook(bookEntity)))
                        .exceptionally(e -> {
                            log.error("Error fetching metadata from provider: {}", provider, e);
                            return null;
                        }))
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        BookMetadata::getProvider,
                        metadata -> metadata,
                        (existing, replacement) -> existing
                ));
    }

    public BookMetadata fetchTopMetadataFromAProvider(MetadataProvider provider, Book book) {
        return getParser(provider).fetchTopMetadata(book, buildFetchMetadataRequestFromBook(book));
    }

    private BookParser getParser(MetadataProvider provider) {
        BookParser parser = parserMap.get(provider);
        if (parser == null) {
            throw ApiError.METADATA_SOURCE_NOT_IMPLEMENT_OR_DOES_NOT_EXIST.createException();
        }
        return parser;
    }

    private FetchMetadataRequest buildFetchMetadataRequestFromBook(Book book) {
        BookMetadata metadata = book.getMetadata();
        return FetchMetadataRequest.builder()
                .isbn(metadata.getIsbn10())
                .asin(metadata.getAsin())
                .author(metadata.getAuthors() != null ? String.join(", ", metadata.getAuthors()) : null)
                .title(metadata.getTitle())
                .bookId(book.getId())
                .build();
    }

    public BookMetadata buildFetchMetadata(Long bookId, MetadataRefreshRequest request, Map<MetadataProvider, BookMetadata> metadataMap) {
        BookMetadata metadata = BookMetadata.builder().bookId(bookId).build();
        MetadataRefreshOptions.FieldOptions fieldOptions = request.getRefreshOptions().getFieldOptions();

        metadata.setTitle(resolveFieldAsString(metadataMap, fieldOptions.getTitle(), BookMetadata::getTitle));
        metadata.setDescription(resolveFieldAsString(metadataMap, fieldOptions.getDescription(), BookMetadata::getDescription));
        metadata.setAuthors(resolveFieldAsList(metadataMap, fieldOptions.getAuthors(), BookMetadata::getAuthors));

        if (metadataMap.containsKey(GoodReads)) {
            metadata.setGoodreadsId(metadataMap.get(GoodReads).getGoodreadsId());
        }
        if (metadataMap.containsKey(Hardcover)) {
            metadata.setHardcoverId(metadataMap.get(Hardcover).getHardcoverId());
        }
        if (metadataMap.containsKey(Google)) {
            metadata.setGoogleId(metadataMap.get(Google).getGoogleId());
        }
        if (metadataMap.containsKey(Comicvine)) {
            metadata.setComicvineId(metadataMap.get(Comicvine).getComicvineId());
        }


        if (request.getRefreshOptions().isMergeCategories()) {
            metadata.setCategories(getAllCategories(metadataMap, fieldOptions.getCategories(), BookMetadata::getCategories));
        } else {
            metadata.setCategories(resolveFieldAsList(metadataMap, fieldOptions.getCategories(), BookMetadata::getCategories));
        }
        metadata.setThumbnailUrl(resolveFieldAsString(metadataMap, fieldOptions.getCover(), BookMetadata::getThumbnailUrl));

        if (request.getRefreshOptions().getAllP4() != null) {
            setOtherUnspecifiedMetadata(metadataMap, metadata, request.getRefreshOptions().getAllP4());
        }
        if (request.getRefreshOptions().getAllP3() != null) {
            setOtherUnspecifiedMetadata(metadataMap, metadata, request.getRefreshOptions().getAllP3());
        }
        if (request.getRefreshOptions().getAllP2() != null) {
            setOtherUnspecifiedMetadata(metadataMap, metadata, request.getRefreshOptions().getAllP2());
        }
        if (request.getRefreshOptions().getAllP1() != null) {
            setOtherUnspecifiedMetadata(metadataMap, metadata, request.getRefreshOptions().getAllP1());
        }

        return metadata;
    }


    protected void setOtherUnspecifiedMetadata(Map<MetadataProvider, BookMetadata> metadataMap, BookMetadata metadataCombined, MetadataProvider provider) {
        if (metadataMap.containsKey(provider)) {
            BookMetadata metadata = metadataMap.get(provider);
            metadataCombined.setSubtitle(metadata.getSubtitle() != null ? metadata.getSubtitle() : metadata.getTitle());
            metadataCombined.setPublisher(metadata.getPublisher() != null ? metadata.getPublisher() : metadataCombined.getPublisher());
            metadataCombined.setPublishedDate(metadata.getPublishedDate() != null ? metadata.getPublishedDate() : metadataCombined.getPublishedDate());
            metadataCombined.setIsbn10(metadata.getIsbn10() != null ? metadata.getIsbn10() : metadataCombined.getIsbn10());
            metadataCombined.setIsbn13(metadata.getIsbn13() != null ? metadata.getIsbn13() : metadataCombined.getIsbn13());
            metadataCombined.setAsin(metadata.getAsin() != null ? metadata.getAsin() : metadataCombined.getAsin());
            metadataCombined.setPageCount(metadata.getPageCount() != null ? metadata.getPageCount() : metadataCombined.getPageCount());
            metadataCombined.setLanguage(metadata.getLanguage() != null ? metadata.getLanguage() : metadataCombined.getLanguage());
            metadataCombined.setGoodreadsRating(metadata.getGoodreadsRating() != null ? metadata.getGoodreadsRating() : metadataCombined.getGoodreadsRating());
            metadataCombined.setGoodreadsReviewCount(metadata.getGoodreadsReviewCount() != null ? metadata.getGoodreadsReviewCount() : metadataCombined.getGoodreadsReviewCount());
            metadataCombined.setAmazonRating(metadata.getAmazonRating() != null ? metadata.getAmazonRating() : metadataCombined.getAmazonRating());
            metadataCombined.setAmazonReviewCount(metadata.getAmazonReviewCount() != null ? metadata.getAmazonReviewCount() : metadataCombined.getAmazonReviewCount());
            metadataCombined.setHardcoverRating(metadata.getHardcoverRating() != null ? metadata.getHardcoverRating() : metadataCombined.getHardcoverRating());
            metadataCombined.setHardcoverReviewCount(metadata.getHardcoverReviewCount() != null ? metadata.getHardcoverReviewCount() : metadataCombined.getHardcoverReviewCount());
            metadataCombined.setPersonalRating(metadata.getPersonalRating() != null ? metadata.getPersonalRating() : metadataCombined.getPersonalRating());
            metadataCombined.setSeriesName(metadata.getSeriesName() != null ? metadata.getSeriesName() : metadataCombined.getSeriesName());
            metadataCombined.setSeriesNumber(metadata.getSeriesNumber() != null ? metadata.getSeriesNumber() : metadataCombined.getSeriesNumber());
            metadataCombined.setSeriesTotal(metadata.getSeriesTotal() != null ? metadata.getSeriesTotal() : metadataCombined.getSeriesTotal());
        }
    }

    @FunctionalInterface
    public interface FieldValueExtractor {
        String extract(BookMetadata metadata);
    }

    @FunctionalInterface
    public interface FieldValueExtractorList {
        Set<String> extract(BookMetadata metadata);
    }


    protected String resolveFieldAsString(Map<MetadataProvider, BookMetadata> metadataMap, MetadataRefreshOptions.FieldProvider fieldProvider, FieldValueExtractor fieldValueExtractor) {
        String value = null;
        if (fieldProvider.getP4() != null && metadataMap.containsKey(fieldProvider.getP4())) {
            String newValue = fieldValueExtractor.extract(metadataMap.get(fieldProvider.getP4()));
            if (newValue != null) value = newValue;
        }
        if (fieldProvider.getP3() != null && metadataMap.containsKey(fieldProvider.getP3())) {
            String newValue = fieldValueExtractor.extract(metadataMap.get(fieldProvider.getP3()));
            if (newValue != null) value = newValue;
        }
        if (fieldProvider.getP2() != null && metadataMap.containsKey(fieldProvider.getP2())) {
            String newValue = fieldValueExtractor.extract(metadataMap.get(fieldProvider.getP2()));
            if (newValue != null) value = newValue;
        }
        if (fieldProvider.getP1() != null && metadataMap.containsKey(fieldProvider.getP1())) {
            String newValue = fieldValueExtractor.extract(metadataMap.get(fieldProvider.getP1()));
            if (newValue != null) value = newValue;
        }
        return value;
    }


    protected Set<String> resolveFieldAsList(Map<MetadataProvider, BookMetadata> metadataMap, MetadataRefreshOptions.FieldProvider fieldProvider, FieldValueExtractorList fieldValueExtractor) {
        Set<String> values = new HashSet<>();
        if (fieldProvider.getP4() != null && metadataMap.containsKey(fieldProvider.getP4())) {
            Set<String> newValues = fieldValueExtractor.extract(metadataMap.get(fieldProvider.getP4()));
            if (newValues != null && !newValues.isEmpty()) values = newValues;
        }
        if (fieldProvider.getP3() != null && metadataMap.containsKey(fieldProvider.getP3())) {
            Set<String> newValues = fieldValueExtractor.extract(metadataMap.get(fieldProvider.getP3()));
            if (newValues != null && !newValues.isEmpty()) values = newValues;
        }
        if (values.isEmpty() && fieldProvider.getP2() != null && metadataMap.containsKey(fieldProvider.getP2())) {
            Set<String> newValues = fieldValueExtractor.extract(metadataMap.get(fieldProvider.getP2()));
            if (newValues != null && !newValues.isEmpty()) values = newValues;
        }
        if (values.isEmpty() && fieldProvider.getP1() != null && metadataMap.containsKey(fieldProvider.getP1())) {
            Set<String> newValues = fieldValueExtractor.extract(metadataMap.get(fieldProvider.getP1()));
            if (newValues != null && !newValues.isEmpty()) values = newValues;
        }
        return values;
    }

    Set<String> getAllCategories(Map<MetadataProvider, BookMetadata> metadataMap, MetadataRefreshOptions.FieldProvider fieldProvider, FieldValueExtractorList fieldValueExtractor) {
        Set<String> uniqueCategories = new HashSet<>();
        if (fieldProvider.getP4() != null && metadataMap.containsKey(fieldProvider.getP4())) {
            Set<String> extracted = fieldValueExtractor.extract(metadataMap.get(fieldProvider.getP4()));
            if (extracted != null) uniqueCategories.addAll(extracted);
        }
        if (fieldProvider.getP3() != null && metadataMap.containsKey(fieldProvider.getP3())) {
            Set<String> extracted = fieldValueExtractor.extract(metadataMap.get(fieldProvider.getP3()));
            if (extracted != null) uniqueCategories.addAll(extracted);
        }
        if (fieldProvider.getP2() != null && metadataMap.containsKey(fieldProvider.getP2())) {
            Set<String> extracted = fieldValueExtractor.extract(metadataMap.get(fieldProvider.getP2()));
            if (extracted != null) uniqueCategories.addAll(extracted);
        }
        if (fieldProvider.getP1() != null && metadataMap.containsKey(fieldProvider.getP1())) {
            Set<String> extracted = fieldValueExtractor.extract(metadataMap.get(fieldProvider.getP1()));
            if (extracted != null) uniqueCategories.addAll(extracted);
        }
        return new HashSet<>(uniqueCategories);
    }


    protected Set<Long> getBookEntities(MetadataRefreshRequest request) {
        MetadataRefreshRequest.RefreshType refreshType = request.getRefreshType();
        if (refreshType != MetadataRefreshRequest.RefreshType.LIBRARY && refreshType != MetadataRefreshRequest.RefreshType.BOOKS) {
            throw ApiError.INVALID_REFRESH_TYPE.createException();
        }
        return switch (refreshType) {
            case LIBRARY -> {
                LibraryEntity libraryEntity = libraryRepository.findById(request.getLibraryId()).orElseThrow(() -> ApiError.LIBRARY_NOT_FOUND.createException(request.getLibraryId()));
                yield bookRepository.findBookIdsByLibraryId(libraryEntity.getId());
            }
            case BOOKS -> request.getBookIds();
        };
    }
}
