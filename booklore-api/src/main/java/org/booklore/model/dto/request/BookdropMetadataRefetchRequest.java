package org.booklore.model.dto.request;

import lombok.Data;
import org.booklore.model.enums.MetadataProvider;

@Data
public class BookdropMetadataRefetchRequest {
    /**
     * If set, fetch top metadata from this single provider only.
     * If null, reprocess using the app's default Quick Book Match settings.
     */
    private MetadataProvider provider;
}
