package org.booklore.config;

import org.booklore.model.dto.BookMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies publishedDate deserialization for the Spring MVC ObjectMapper.
 *
 * Mirrors the auto-configured mapper (JsonMapper + all classpath modules).
 * Run once without FlexibleLocalDateDeserializer to confirm year/year-month
 * formats fail, then again after the deserializer is wired in.
 */
class BookMetadataPublishedDateDeserializationTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setup() {
        mapper = JsonMapper.builder()
                .findAndAddModules()
                .build();
    }

    @Test
    void iso8601_full_date_deserializes() throws Exception {
        String json = "{\"publishedDate\":\"2020-01-15\"}";
        BookMetadata meta = mapper.readValue(json, BookMetadata.class);
        assertThat(meta.getPublishedDate()).isEqualTo(LocalDate.of(2020, 1, 15));
    }

    @Test
    void year_month_deserializes_to_first_of_month() throws Exception {
        String json = "{\"publishedDate\":\"2020-01\"}";
        BookMetadata meta = mapper.readValue(json, BookMetadata.class);
        assertThat(meta.getPublishedDate()).isEqualTo(LocalDate.of(2020, 1, 1));
    }

    @Test
    void year_only_deserializes_to_january_first() throws Exception {
        String json = "{\"publishedDate\":\"2020\"}";
        BookMetadata meta = mapper.readValue(json, BookMetadata.class);
        assertThat(meta.getPublishedDate()).isEqualTo(LocalDate.of(2020, 1, 1));
    }

    @Test
    void null_value_returns_null() throws Exception {
        String json = "{\"publishedDate\":null}";
        BookMetadata meta = mapper.readValue(json, BookMetadata.class);
        assertThat(meta.getPublishedDate()).isNull();
    }

    @Test
    void empty_string_returns_null() throws Exception {
        String json = "{\"publishedDate\":\"\"}";
        BookMetadata meta = mapper.readValue(json, BookMetadata.class);
        assertThat(meta.getPublishedDate()).isNull();
    }
}
