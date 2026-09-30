package org.booklore.model.dto.notes;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * On-disk representation of the notes for one book, stored next to the book file
 * as {@code <book name>.notes.json}. The file holds the notes of every user; each
 * entry records its owner by username so the file stays meaningful if the database
 * is ever rebuilt.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BookNoteFile {

    @Builder.Default
    private int version = 1;

    @Builder.Default
    private List<Entry> notes = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Entry {
        private String id;
        private String owner;
        private String title;
        private String content;
        private Instant createdAt;
        private Instant updatedAt;
    }
}
