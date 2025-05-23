package com.adityachandel.booklore.model.dto;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class BookViewerSettings {
    private PdfViewerPreferences pdfSettings;
    private EpubViewerPreferences epubSettings;
    private CbxViewerPreferences cbxSettings;
}