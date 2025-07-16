package com.adityachandel.booklore.service.fileprocessor;

import com.adityachandel.booklore.model.enums.BookFileExtension;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@Slf4j
public class BookFileProcessorRegistry {
    
    private final Map<BookFileExtension, BookFileProcessor> processorMap;
    
    public BookFileProcessorRegistry(List<BookFileProcessor> processors) {
        this.processorMap = new EnumMap<>(BookFileExtension.class);
        initializeProcessorMap(processors);
    }
    
    private void initializeProcessorMap(List<BookFileProcessor> processors) {
        for (BookFileProcessor processor : processors) {
            List<BookFileExtension> supportedExtensions = processor.getSupportedExtensions();
            for (BookFileExtension extension : supportedExtensions) {
                processorMap.put(extension, processor);
                log.debug("Registered {} for extension: {}", processor.getClass().getSimpleName(), extension);
            }
        }
        log.info("Initialized BookFileProcessorRegistry with {} processors for {} extensions", 
                processors.size(), processorMap.size());
    }
    
    public Optional<BookFileProcessor> getProcessor(BookFileExtension extension) {
        return Optional.ofNullable(processorMap.get(extension));
    }
    
    public BookFileProcessor getProcessorOrThrow(BookFileExtension extension) {
        return getProcessor(extension)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No processor found for file extension: " + extension));
    }
}