package com.adityachandel.booklore.service.metadata;

import com.adityachandel.booklore.model.dto.BookMetadata;

import java.util.List;

@FunctionalInterface
interface FieldValueExtractorList {
    List<String> extract(BookMetadata metadata);
}
