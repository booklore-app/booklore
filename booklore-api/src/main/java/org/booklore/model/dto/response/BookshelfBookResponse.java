package org.booklore.model.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BookshelfBookResponse {
    private Long id;
    private String isbn;
    private String title;
    private String author;
    private String description;
    @JsonProperty("cover_url")
    private String coverUrl;
    @JsonProperty("published_date")
    private String publishedDate;
    @JsonProperty("page_count")
    private Integer pageCount;
    private String publisher;
    private String genre;
    @JsonProperty("sub_genre")
    private String subGenre;
    private String language;
    @JsonProperty("series_name")
    private String seriesName;
    @JsonProperty("series_number")
    private String seriesNumber;
    @JsonProperty("amazon_asin")
    private String amazonAsin;
}
