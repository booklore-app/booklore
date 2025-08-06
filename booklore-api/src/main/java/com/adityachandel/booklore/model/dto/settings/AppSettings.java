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
    private String coverResolution;
    private MetadataRefreshOptions metadataRefreshOptions;
    private boolean autoBookSearch;
    private boolean similarBookRecommendation;
    private boolean opdsServerEnabled;
    private String uploadPattern;
    private String movePattern;
    private Integer cbxCacheSizeInMb;
    private Integer pdfCacheSizeInMb;
    private Integer maxFileUploadSizeInMb;
    private boolean remoteAuthEnabled;
    private boolean oidcEnabled;
    private boolean bookDeletionEnabled;
    private boolean metadataDownloadOnBookdrop;
    private OidcProviderDetails oidcProviderDetails;
    private OidcAutoProvisionDetails oidcAutoProvisionDetails;
    private MetadataProviderSettings metadataProviderSettings;
    private MetadataMatchWeights metadataMatchWeights;
    private MetadataPersistenceSettings metadataPersistenceSettings;
}