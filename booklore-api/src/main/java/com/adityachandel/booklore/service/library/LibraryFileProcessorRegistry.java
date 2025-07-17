package com.adityachandel.booklore.service.library;

import com.adityachandel.booklore.model.entity.LibraryEntity;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@AllArgsConstructor
@Component
public class LibraryFileProcessorRegistry {

    private final FileAsBookProcessor fileAsBookProcessor;

    public LibraryFileProcessor getProcessor(LibraryEntity library) {
        return fileAsBookProcessor;
    }
}
