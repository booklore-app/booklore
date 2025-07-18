package com.adityachandel.booklore.service.fileprocessor;

import com.adityachandel.booklore.model.dto.Book;
import com.adityachandel.booklore.model.dto.settings.LibraryFile;

public interface FileProcessor {
    Book processFile(LibraryFile libraryFile);
}
