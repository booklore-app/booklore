package com.adityachandel.booklore.model.dto.request;

import com.adityachandel.booklore.model.dto.BookMetadata;
import lombok.Data;

import java.util.List;

@Data
public class BookdropFinalizeRequest {
    private String uploadPattern;
    private Boolean selectAll;
    private List<Long> excludedIds;
    private List<BookdropFinalizeFile> files;
    private Long defaultLibraryId;
    private Long defaultPathId;

    @Data
    public static class BookdropFinalizeFile {
        private Long fileId;
        private Long libraryId;
        private Long pathId;
        private BookMetadata metadata;
    }
}