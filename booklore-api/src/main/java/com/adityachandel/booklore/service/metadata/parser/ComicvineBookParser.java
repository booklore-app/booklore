package com.adityachandel.booklore.service.metadata.parser;

import com.adityachandel.booklore.model.dto.Book;
import com.adityachandel.booklore.model.dto.BookMetadata;
import com.adityachandel.booklore.model.dto.request.FetchMetadataRequest;
import com.adityachandel.booklore.model.enums.MetadataProvider;
import com.adityachandel.booklore.service.appsettings.AppSettingService;
import com.adityachandel.booklore.util.BookUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import com.adityachandel.booklore.model.dto.response.ComicvineApiResponse;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class ComicvineBookParser implements BookParser {

    
    private final ObjectMapper objectMapper;
    private static final String COMICVINE_URL = "https://comicvine.gamespot.com/api/";
    //TODO: CHANGE THIS!!!
    private final AppSettingService appSettingService;
    //private static final String API_KEY = "93e4425f66f8fbe6db5bd8c74d31c4defc2d27e9";



    @Override
    public List<BookMetadata> fetchMetadata(Book book, FetchMetadataRequest fetchMetadataRequest) {
        String searchTerm = getSearchTerm(book, fetchMetadataRequest);
        return searchTerm != null ? getMetadataListByTerm(searchTerm) : List.of();
    }

    public List<BookMetadata> getMetadataListByTerm(String term) {
        String apiToken = appSettingService.getAppSettings().getMetadataProviderSettings().getComicvine().getApiKey();
        if (apiToken == null || apiToken.isEmpty()) {
            log.warn("Comicvine API token not set");
            return Collections.emptyList();
        }
        log.info("Comicvine: Fetching metadata for: {}", term);
        try {
            String fieldsList = "api_detail_url,description,id,image,name,publisher,start_year";
            URI uri = UriComponentsBuilder.fromUriString(COMICVINE_URL) // Base URL
                    .path("/volumes/")
                    .queryParam("api_key", apiToken) // Replace with your actual API key
                    .queryParam("format", "json")
                    .queryParam("query", term)
                    .queryParam("filter", "name:" + term)
                    .queryParam("limit", "10") // Limit results to reduce response size
                    .queryParam("field_list", fieldsList)
                    .build()
                    .toUri();
            HttpClient httpClient = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .header("User-Agent", "MyComicApp/1.0")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return parseComicvineApiResponse(response.body());
            } else {
                log.error("Failed to fetch data from Comicvine API. Status code: {}", response.statusCode());
                return List.of();
            }
        } catch (IOException | InterruptedException e) {
            log.error("Error fetching metadata from Comicvine API", e);
            return List.of();
        }
    }



    @Override
    public BookMetadata fetchTopMetadata(Book book, FetchMetadataRequest fetchMetadataRequest) {
        List<BookMetadata> fetchedBookMetadata = fetchMetadata(book, fetchMetadataRequest);
        return fetchedBookMetadata.isEmpty() ? null : fetchedBookMetadata.get(0);
    }

     private String getSearchTerm(Book book, FetchMetadataRequest request) {
        return (request.getTitle() != null && !request.getTitle().isEmpty())
                ? request.getTitle()
                : (book.getFileName() != null && !book.getFileName().isEmpty()
                ? BookUtils.cleanFileName(book.getFileName())
                : null);
    }

    private List<BookMetadata> parseComicvineApiResponse(String responseBody) throws IOException {
        ComicvineApiResponse apiResponse = objectMapper.readValue(responseBody, ComicvineApiResponse.class);
        return apiResponse.getResults().stream()
                .map(this::convertToBookMetadata)
                .collect(Collectors.toList());
    }

    private BookMetadata convertToBookMetadata(ComicvineApiResponse.Comic comic) {
    return BookMetadata.builder()
            .title(comic.getName())
            .comicvineId(String.valueOf(comic.getId()))
            .description(comic.getDescription())
            .publisher(comic.getPublisherName())      // Custom method
            .thumbnailUrl(comic.getImageUrl())        // Custom method
            .provider(MetadataProvider.Comicvine)
            .publishedDate(parsePublishedDate(comic.getStartYear())) // Now camelCase
            .build();
}

    private LocalDate parsePublishedDate(String startYear) {
        if (startYear == null || startYear.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(startYear + "-01-01", DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (Exception e) {
            return null;
        }
    }




}
