package com.adityachandel.booklore.model.dto.request;

import lombok.Data;

import java.time.LocalDate;
import java.util.Set;

@Data
public class BulkMetadataUpdateRequest {

    private Set<Long> bookIds;

    private Set<String> authors;
    private String publisher;
    private String language;
    private String seriesName;
    private Integer seriesTotal;
    private LocalDate publishedDate;
    private Set<String> genres;
}
