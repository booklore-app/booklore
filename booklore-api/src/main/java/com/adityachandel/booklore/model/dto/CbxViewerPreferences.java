package com.adityachandel.booklore.model.dto;

import com.adityachandel.booklore.model.enums.CbxPageSpread;
import com.adityachandel.booklore.model.enums.CbxPageViewMode;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CbxViewerPreferences {
    private Long bookId;
    private CbxPageSpread pageSpread;
    private CbxPageViewMode pageViewMode;
}