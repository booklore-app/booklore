package com.adityachandel.booklore.service.metadata.parser;

import com.adityachandel.booklore.model.dto.Book;
import com.adityachandel.booklore.model.dto.BookMetadata;
import com.adityachandel.booklore.model.dto.request.FetchMetadataRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class ParserSearchTermTest {

    private Book comicBook() {
        return Book.builder()
                .fileName("renamed.cbz")
                .metadata(BookMetadata.builder()
                        .seriesName("SeriesName")
                        .seriesNumber(1f)
                        .build())
                .build();
    }

    @Test
    void comicvineUsesSeriesSearchTermWhenFileRenamed() {
        Book book = comicBook();
        FetchMetadataRequest request = FetchMetadataRequest.builder().build();
        ComicvineBookParser parser = new ComicvineBookParser(new ObjectMapper(), null);
        String term = ReflectionTestUtils.invokeMethod(parser, "getSearchTerm", book, request);
        assertThat(term).isEqualTo("SeriesName #1");
    }

    @Test
    void goodreadsUsesSeriesSearchTermWhenFileRenamed() {
        Book book = comicBook();
        FetchMetadataRequest request = FetchMetadataRequest.builder().build();
        GoodReadsParser parser = new GoodReadsParser();
        String term = ReflectionTestUtils.invokeMethod(parser, "getSearchTerm", book, request);
        assertThat(term).isEqualTo("SeriesName #1");
    }

    @Test
    void googleUsesSeriesSearchTermWhenFileRenamed() {
        Book book = comicBook();
        FetchMetadataRequest request = FetchMetadataRequest.builder().build();
        GoogleParser parser = new GoogleParser(new ObjectMapper());
        String term = ReflectionTestUtils.invokeMethod(parser, "getSearchTerm", book, request);
        assertThat(term).isEqualTo("SeriesName #1");
    }
}
