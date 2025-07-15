package com.adityachandel.booklore.model.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.util.List;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ComicvineApiResponse {
    private String error;
    private int limit;
    private int offset;
    
    @JsonProperty("number_of_page_results")
    private int numberOfPageResults;
    
    @JsonProperty("number_of_total_results")
    private int numberOfTotalResults;
    
    @JsonProperty("status_code")
    private int statusCode;
    
    private List<Comic> results;
    private String version;


    /**
     * Represents a response from the Comicvine API.
     * This class contains metadata about the response and a list of comic results.
     */


    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Comic {
        @JsonProperty("api_detail_url")
        private String apiDetailUrl;
        
        private String description;
        private int id;
        private ComicVineImage image;
        private String name;
        private Publisher publisher;
        
        @JsonProperty("site_detail_url")
        private String siteDetailUrl;
        
        @JsonProperty("start_year")
        private String startYear;

        // Custom getter for image URL
        public String getImageUrl() {
            if (image == null) {
                System.out.println("Image object is null!");
                return null;
            }
            String thumbUrl = image.getOriginalUrl() != null ? image.getOriginalUrl() : image.getThumbUrl();
            System.out.println("Image URL: " + thumbUrl);
            return thumbUrl;
        }

        // Custom getter for publisher name
        public String getPublisherName() {
            return publisher != null ? publisher.getName() : null;
        }

        public String getDescription() {
            if (description == null || description.isEmpty()) {
                return "No description available.";

            }
            String html = description.replaceAll("\\\\u003C", "<")
                             .replaceAll("\\\\u003E", ">")
                             .replaceAll("\\\\\"", "\"");
            // Clean the description by removing HTML tags
            Document doc = Jsoup.parse(html);
            return doc.text();
        }
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ComicVineImage {
        @JsonProperty("icon_url")
        private String iconUrl;
        
        @JsonProperty("medium_url")
        private String mediumUrl;
        
        @JsonProperty("screen_url")
        private String screenUrl;
        
        @JsonProperty("screen_large_url")
        private String screenLargeUrl;
        
        @JsonProperty("small_url")
        private String smallUrl;
        
        @JsonProperty("super_url")
        private String superUrl;
        
        @JsonProperty("thumb_url")
        private String thumbUrl;
        
        @JsonProperty("tiny_url")
        private String tinyUrl;
        
        @JsonProperty("original_url")
        private String originalUrl;
        
        @JsonProperty("image_tags")
        private String imageTags;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Publisher {
        @JsonProperty("api_detail_url")
        private String apiDetailUrl;
        
        private int id;
        private String name;
    }

    // Keep this method for backward compatibility
    public List<Comic> getData() {
        return results;
    }
}
