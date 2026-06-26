package org.booklore.model.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenLibraryWorkResponse {
    private String key;
    private String title;
    private Object description;
    private List<String> subjects;
    private List<Integer> covers;
    @JsonProperty("first_publish_date")
    private String firstPublishDate;
}
