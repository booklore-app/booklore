package com.adityachandel.booklore.service.fileprocessor;

import com.adityachandel.booklore.model.dto.Book;
import com.adityachandel.booklore.model.dto.settings.LibraryFile;
import com.adityachandel.booklore.model.entity.BookEntity;
import com.adityachandel.booklore.model.enums.BookFileExtension;

import java.util.List;

public interface BookFileProcessor {
    List<BookFileExtension> getSupportedExtensions();
    Book processFile(LibraryFile libraryFile);
    boolean generateCover(BookEntity bookEntity);
}
