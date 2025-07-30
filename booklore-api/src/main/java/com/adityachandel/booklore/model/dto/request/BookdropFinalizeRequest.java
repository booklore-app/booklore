package com.adityachandel.booklore.model.dto.request;

import com.adityachandel.booklore.model.dto.BookMetadata;
import lombok.Data;

import java.util.List;

@Data
public class BookdropFinalizeRequest {
    private String uploadPattern;
    private List<BookdropFinalizeFile> files;

    @Data
    public static class BookdropFinalizeFile {
        private Long fileId;
        private Long libraryId;
        private Long pathId;
        private BookMetadata metadata;
    }
}
