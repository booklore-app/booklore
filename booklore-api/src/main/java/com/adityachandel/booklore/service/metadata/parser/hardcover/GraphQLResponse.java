package com.adityachandel.booklore.service.metadata.parser.hardcover;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GraphQLResponse {
    private DataWrapper data;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DataWrapper {
        private SearchWrapper search;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SearchWrapper {
        private ResultsWrapper results;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ResultsWrapper {
        private List<Hit> hits;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Hit {
        private Document document;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Document {
        private String id;
        private String slug;
        private String title;

        @JsonProperty("author_names")
        private List<String> authorNames;

        private String description;
        private List<String> isbns;
        private Double rating;

        @JsonProperty("ratings_count")
        private Integer ratingsCount;

        private Integer pages;

        @JsonProperty("release_date")
        private String releaseDate;

        private List<String> genres;

        @JsonProperty("featured_series")
        private FeaturedSeries featuredSeries;

        private Image image;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Image {
        private String url;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FeaturedSeries {
        private Integer position;
        private Series series;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Series {
        private String name;
        @JsonProperty("books_count")
        private Integer booksCount;
    }
}

