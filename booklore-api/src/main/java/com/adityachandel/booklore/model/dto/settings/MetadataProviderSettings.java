package com.adityachandel.booklore.model.dto.settings;

import lombok.Data;

@Data
public class MetadataProviderSettings {
    private Amazon amazon;
    private Google google;
    private Goodreads goodReads;
    private Hardcover hardcover;

    @Data
    public static class Amazon {
        private boolean enabled;
        private String cookie;
    }

    @Data
    public static class Google {
        private boolean enabled;
    }

    @Data
    public static class Goodreads {
        private boolean enabled;
    }

    @Data
    public static class Hardcover {
        private boolean enabled;
        private String apiKey;
    }
}
