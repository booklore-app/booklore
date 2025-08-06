package com.adityachandel.booklore.service.bookdrop;

import com.adityachandel.booklore.exception.ApiError;
import com.adityachandel.booklore.model.dto.Book;
import com.adityachandel.booklore.model.dto.BookMetadata;
import com.adityachandel.booklore.model.dto.request.MetadataRefreshRequest;
import com.adityachandel.booklore.model.dto.settings.AppSettings;
import com.adityachandel.booklore.model.entity.BookdropFileEntity;
import com.adityachandel.booklore.model.enums.BookFileExtension;
import com.adityachandel.booklore.model.enums.MetadataProvider;
import com.adityachandel.booklore.repository.BookdropFileRepository;
import com.adityachandel.booklore.service.appsettings.AppSettingService;
import com.adityachandel.booklore.service.metadata.MetadataRefreshService;
import com.adityachandel.booklore.service.metadata.extractor.CbxMetadataExtractor;
import com.adityachandel.booklore.service.metadata.extractor.EpubMetadataExtractor;
import com.adityachandel.booklore.service.metadata.extractor.PdfMetadataExtractor;
import com.adityachandel.booklore.util.FileService;
import com.adityachandel.booklore.util.ImageUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static com.adityachandel.booklore.model.entity.BookdropFileEntity.Status.PENDING_REVIEW;

@Slf4j
@AllArgsConstructor
@Service
public class BookdropMetadataService {

    private final BookdropFileRepository bookdropFileRepository;
    private final AppSettingService appSettingService;
    private final ObjectMapper objectMapper;
    private final EpubMetadataExtractor epubMetadataExtractor;
    private final PdfMetadataExtractor pdfMetadataExtractor;
    private final CbxMetadataExtractor cbxMetadataExtractor;
    private final MetadataRefreshService metadataRefreshService;
    private final ImageUtils imageUtils;
    private final FileService fileService;

    @Transactional
    public BookdropFileEntity attachInitialMetadata(Long bookdropFileId) throws JsonProcessingException {
        BookdropFileEntity entity = getOrThrow(bookdropFileId);
        BookMetadata initial = extractInitialMetadata(entity);
        extractAndSaveCover(entity);
        String initialJson = objectMapper.writeValueAsString(initial);
        entity.setOriginalMetadata(initialJson);
        entity.setUpdatedAt(Instant.now());
        return bookdropFileRepository.save(entity);
    }

    @Transactional
    public BookdropFileEntity attachFetchedMetadata(Long bookdropFileId) throws JsonProcessingException {
        BookdropFileEntity entity = getOrThrow(bookdropFileId);

        AppSettings appSettings = appSettingService.getAppSettings();
        MetadataRefreshRequest request = MetadataRefreshRequest.builder()
                .refreshOptions(appSettings.getMetadataRefreshOptions())
                .build();

        BookMetadata initial = objectMapper.readValue(entity.getOriginalMetadata(), BookMetadata.class);

        List<MetadataProvider> providers = metadataRefreshService.prepareProviders(request);
        Book book = Book.builder()
                .fileName(entity.getFileName())
                .metadata(initial)
                .build();

        if (providers.contains(MetadataProvider.GoodReads)) {
            try {
                Thread.sleep(ThreadLocalRandom.current().nextLong(250, 1250));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        Map<MetadataProvider, BookMetadata> metadataMap = metadataRefreshService.fetchMetadataForBook(providers, book);
        BookMetadata fetchedMetadata = metadataRefreshService.buildFetchMetadata(book.getId(), request, metadataMap);
        String fetchedJson = objectMapper.writeValueAsString(fetchedMetadata);

        entity.setFetchedMetadata(fetchedJson);
        entity.setStatus(PENDING_REVIEW);
        entity.setUpdatedAt(Instant.now());

        return bookdropFileRepository.save(entity);
    }

    private BookdropFileEntity getOrThrow(Long id) {
        return bookdropFileRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Bookdrop file not found: " + id));
    }

    private BookMetadata extractInitialMetadata(BookdropFileEntity entity) {
        File file = new File(entity.getFilePath());
        BookFileExtension fileExt = BookFileExtension.fromFileName(file.getName()).orElseThrow(() -> ApiError.INVALID_FILE_FORMAT.createException("Unsupported file extension"));
        return switch (fileExt) {
            case PDF -> pdfMetadataExtractor.extractMetadata(file);
            case EPUB -> epubMetadataExtractor.extractMetadata(file);
            case CBZ, CBR, CB7 -> cbxMetadataExtractor.extractMetadata(file);
        };
    }

    private void extractAndSaveCover(BookdropFileEntity entity) {
        File file = new File(entity.getFilePath());
        BookFileExtension fileExt = BookFileExtension.fromFileName(file.getName()).orElseThrow(() -> ApiError.INVALID_FILE_FORMAT.createException("Unsupported file extension"));
        byte[] coverBytes;
        coverBytes = switch (fileExt) {
            case EPUB -> epubMetadataExtractor.extractCover(file);
            case PDF -> pdfMetadataExtractor.extractCover(file);
            case CBZ, CBR, CB7 -> cbxMetadataExtractor.extractCover(file);
        };
        if (coverBytes != null) {
            try {
                imageUtils.saveImage(coverBytes, fileService.getTempBookdropCoverImagePath(entity.getId()), 250, 350);
            } catch (IOException e) {
                log.warn("Failed to save extracted cover for file: {}", entity.getFilePath(), e);
            }
        }
    }
}
