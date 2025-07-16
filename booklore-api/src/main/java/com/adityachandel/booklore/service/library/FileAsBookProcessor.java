package com.adityachandel.booklore.service.library;

import com.adityachandel.booklore.model.dto.Book;
import com.adityachandel.booklore.model.dto.settings.LibraryFile;
import com.adityachandel.booklore.model.entity.LibraryEntity;
import com.adityachandel.booklore.model.websocket.Topic;
import com.adityachandel.booklore.service.NotificationService;
import com.adityachandel.booklore.service.fileprocessor.CbxProcessor;
import com.adityachandel.booklore.service.fileprocessor.EpubProcessor;
import com.adityachandel.booklore.service.fileprocessor.PdfProcessor;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.adityachandel.booklore.model.websocket.LogNotification.createLogNotification;

@AllArgsConstructor
@Component
@Slf4j
public class FileAsBookProcessor implements LibraryFileProcessor {
    
    private final NotificationService notificationService;
    private final PdfProcessor pdfProcessor;
    private final EpubProcessor epubProcessor;
    private final CbxProcessor cbxProcessor;

    @Override
    @Transactional
    public void processLibraryFiles(List<LibraryFile> libraryFiles, LibraryEntity libraryEntity) {
        for (LibraryFile libraryFile : libraryFiles) {
            log.info("Processing file: {}", libraryFile.getFileName());
            Book book = processLibraryFile(libraryFile);
            if (book != null) {
                notificationService.sendMessage(Topic.BOOK_ADD, book);
                notificationService.sendMessage(Topic.LOG, createLogNotification("Book added: " + book.getFileName()));
                log.info("Processed file: {}", libraryFile.getFileName());
            }
        }
    }

    @Transactional
    protected Book processLibraryFile(LibraryFile libraryFile) {
        return switch (libraryFile.getBookFileType()) {
            case PDF -> pdfProcessor.processFile(libraryFile);
            case EPUB -> epubProcessor.processFile(libraryFile);
            case CBX -> cbxProcessor.processFile(libraryFile);
        };
    }
}