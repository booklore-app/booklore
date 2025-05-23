package com.adityachandel.booklore.service;

import com.adityachandel.booklore.model.UploadedFileMetadata;

public interface UploadedFileMetadataExtractor {

    UploadedFileMetadata extractMetadata(String filePath);
}
