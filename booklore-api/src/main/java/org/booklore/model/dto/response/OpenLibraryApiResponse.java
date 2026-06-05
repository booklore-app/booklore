package org.booklore.model.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenLibraryApiResponse {
    private List<Doc> docs;
    private Integer numFound;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Doc {
        private String key;
        private String title;
        @JsonProperty("author_name")
        private List<String> authorName;
        @JsonProperty("first_publish_year")
        private Integer firstPublishYear;
        private List<String> isbn;
        @JsonProperty("cover_i")
        private Integer coverI;
        private List<String> publisher;
        private List<String> language;
        @JsonProperty("number_of_pages_median")
        private Integer numberOfPagesMedian;
        private List<String> subject;
    }
}
