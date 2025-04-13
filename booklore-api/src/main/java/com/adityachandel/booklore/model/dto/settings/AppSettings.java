package com.adityachandel.booklore.model.dto.settings;

import com.adityachandel.booklore.model.dto.request.MetadataRefreshOptions;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AppSettings {
    private CoverSettings coverSettings;
    private MetadataRefreshOptions metadataRefreshOptions;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CoverSettings {
        private String resolution;
    }
}
