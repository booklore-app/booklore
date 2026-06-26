package org.booklore.service.metadata.parser;

import org.booklore.model.dto.Book;
import org.booklore.model.dto.BookMetadata;
import org.booklore.model.dto.request.FetchMetadataRequest;
import org.booklore.model.enums.MetadataProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OpenLibraryParserTest {

    private HttpClient httpClient;
    private OpenLibraryParser parser;

    @BeforeEach
    void setUp() {
        httpClient = mock(HttpClient.class);
        parser = new OpenLibraryParser(new ObjectMapper(), httpClient);
    }

    @Test
    void fetchMetadata_mapsSearchDocs() throws Exception {
        mockResponse("""
                {
                  "numFound": 1,
                  "docs": [
                    {
                      "key": "/works/OL12345W",
                      "title": "The Great Gatsby",
                      "author_name": ["F. Scott Fitzgerald"],
                      "first_publish_year": 1925,
                      "isbn": ["1234567890", "1234567890123"],
                      "cover_i": 12345,
                      "publisher": ["Charles Scribner's Sons"],
                      "language": ["eng"],
                      "number_of_pages_median": 180,
                      "subject": ["Fiction", "Classic"]
                    }
                  ]
                }
                """);

        List<BookMetadata> results = parser.fetchMetadata(Book.builder().build(), FetchMetadataRequest.builder()
                .title("The Great Gatsby")
                .author("Fitzgerald")
                .build());

        assertEquals(1, results.size());
        BookMetadata metadata = results.getFirst();
        assertEquals(MetadataProvider.OpenLibrary, metadata.getProvider());
        assertEquals("The Great Gatsby", metadata.getTitle());
        assertEquals(List.of("F. Scott Fitzgerald"), metadata.getAuthors());
        assertEquals(LocalDate.of(1925, 1, 1), metadata.getPublishedDate());
        assertEquals("1234567890", metadata.getIsbn10());
        assertEquals("1234567890123", metadata.getIsbn13());
        assertEquals("Charles Scribner's Sons", metadata.getPublisher());
        assertEquals("eng", metadata.getLanguage());
        assertEquals(180, metadata.getPageCount());
        assertTrue(metadata.getCategories().contains("Fiction"));
        assertEquals("https://covers.openlibrary.org/b/id/12345-L.jpg", metadata.getThumbnailUrl());
        assertEquals("https://openlibrary.org/works/OL12345W", metadata.getExternalUrl());
    }

    @Test
    void fetchMetadata_prioritizesIsbnSearch() throws Exception {
        mockResponse("{\"docs\": []}");

        parser.fetchMetadata(Book.builder().build(), FetchMetadataRequest.builder()
                .isbn("978-1-234-56789-7")
                .title("Ignored Title")
                .build());

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(captor.capture(), any(HttpResponse.BodyHandler.class));

        String uri = captor.getValue().uri().toString();
        assertTrue(uri.contains("isbn=9781234567897"));
        assertTrue(!uri.contains("title=Ignored"));
    }

    @Test
    void fetchTopMetadata_enrichesSearchResultWithWorkDetails() throws Exception {
        mockResponseSequence(
                """
                {
                  "docs": [
                    {
                      "key": "/works/OL1W",
                      "title": "Search Title",
                      "author_name": ["Author One"],
                      "first_publish_year": 1999,
                      "isbn": ["1234567890"],
                      "subject": ["Search Subject"]
                    }
                  ]
                }
                """,
                """
                {
                  "key": "/works/OL1W",
                  "title": "Work Title",
                  "description": {"type": "/type/text", "value": "<p>Work description.</p>"},
                  "first_publish_date": "2001",
                  "subjects": ["Work Subject", "Another Subject"],
                  "covers": [98765]
                }
                """
        );

        BookMetadata metadata = parser.fetchTopMetadata(Book.builder().build(), FetchMetadataRequest.builder()
                .title("Search Title")
                .build());

        assertEquals("Work Title", metadata.getTitle());
        assertEquals("Work description.", metadata.getDescription());
        assertEquals(LocalDate.of(2001, 1, 1), metadata.getPublishedDate());
        assertEquals(List.of("Author One"), metadata.getAuthors());
        assertEquals("1234567890", metadata.getIsbn10());
        assertTrue(metadata.getCategories().contains("Work Subject"));
        assertEquals("https://covers.openlibrary.org/b/id/98765-L.jpg", metadata.getThumbnailUrl());
        verify(httpClient, times(2)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void fetchMetadata_returnsEmptyWhenNoTitleOrIsbn() {
        List<BookMetadata> results = parser.fetchMetadata(Book.builder().build(), FetchMetadataRequest.builder()
                .author("Author")
                .build());

        assertTrue(results.isEmpty());
        assertNull(parser.fetchTopMetadata(Book.builder().build(), FetchMetadataRequest.builder().build()));
    }

    private void mockResponse(String jsonBody) throws IOException, InterruptedException {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.body()).thenReturn(jsonBody);
        when(response.statusCode()).thenReturn(200);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
    }

    private void mockResponseSequence(String... jsonBodies) throws IOException, InterruptedException {
        HttpResponse<String>[] responses = new HttpResponse[jsonBodies.length];
        for (int i = 0; i < jsonBodies.length; i++) {
            HttpResponse<String> response = mock(HttpResponse.class);
            when(response.body()).thenReturn(jsonBodies[i]);
            when(response.statusCode()).thenReturn(200);
            responses[i] = response;
        }
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(responses[0], Arrays.copyOfRange(responses, 1, responses.length));
    }
}
