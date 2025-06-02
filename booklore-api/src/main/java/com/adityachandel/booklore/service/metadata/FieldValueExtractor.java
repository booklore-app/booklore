package com.adityachandel.booklore.service.metadata;

import com.adityachandel.booklore.model.dto.BookMetadata;

@FunctionalInterface
interface FieldValueExtractor {
    String extract(BookMetadata metadata);
}
