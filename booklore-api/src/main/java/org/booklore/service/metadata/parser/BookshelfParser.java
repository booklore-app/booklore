package org.booklore.service.metadata.parser;

import lombok.extern.slf4j.Slf4j;
import org.booklore.model.dto.Book;
import org.booklore.model.dto.BookFile;
import org.booklore.model.dto.BookMetadata;
import org.booklore.model.dto.request.FetchMetadataRequest;
import org.booklore.model.dto.response.BookshelfBookResponse;
import org.booklore.model.dto.response.BookshelfSearchResponse;
import org.booklore.model.enums.MetadataProvider;
import org.booklore.service.appsettings.AppSettingService;
import org.booklore.util.BookUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

@Slf4j
@Service
public class BookshelfParser implements BookParser {

    private static final String BASE_URL = "https://bookshelf.nz";
    private static final String API_KEY_HEADER = "X-Catalog-Api-Key";
    private static final int SEARCH_LIMIT = 10;
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");
    private static final Pattern YEAR_PATTERN = Pattern.compile("^(\\d{4})");
    private static final Pattern ASIN_PATTERN = Pattern.compile("^[A-Z0-9]{10}$");

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final AppSettingService appSettingService;

    @Autowired
    public BookshelfParser(ObjectMapper objectMapper, AppSettingService appSettingService) {
        this(objectMapper, HttpClient.newHttpClient(), appSettingService);
    }

    public BookshelfParser(ObjectMapper objectMapper, HttpClient httpClient, AppSettingService appSettingService) {
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
        this.appSettingService = appSettingService;
    }

    private HttpRequest.Builder authorizedRequestBuilder(URI uri) {
        HttpRequest.Builder builder = HttpRequest.newBuilder().uri(uri);
        String apiKey = appSettingService.getAppSettings().getMetadataProviderSettings().getBookshelf() != null
                ? appSettingService.getAppSettings().getMetadataProviderSettings().getBookshelf().getApiKey()
                : null;
        if (apiKey != null && !apiKey.isBlank()) {
            builder.header(API_KEY_HEADER, apiKey.trim());
        }
        return builder;
    }

    @Override
    public List<BookMetadata> fetchMetadata(Book book, FetchMetadataRequest fetchMetadataRequest) {
        // Bookshelf stores Kindle-exclusive titles by ASIN in the same "isbn" lookup slot,
        // so fall back to the ASIN when no real ISBN is available.
        String identifier = ParserUtils.cleanIsbn(fetchMetadataRequest.getIsbn());
        if (identifier == null || identifier.isBlank()) {
            String asin = fetchMetadataRequest.getAsin();
            identifier = (asin != null && !asin.isBlank()) ? asin.trim().toUpperCase() : null;
        }

        if (identifier != null && !identifier.isBlank()) {
            BookMetadata byIdentifier = fetchByIsbn(identifier);
            if (byIdentifier != null) {
                return List.of(byIdentifier);
            }
        }
        return fetchBySearch(book, fetchMetadataRequest);
    }

    @Override
    public BookMetadata fetchTopMetadata(Book book, FetchMetadataRequest fetchMetadataRequest) {
        List<BookMetadata> results = fetchMetadata(book, fetchMetadataRequest);
        return results.isEmpty() ? null : results.getFirst();
    }

    private BookMetadata fetchByIsbn(String isbn) {
        URI uri = URI.create(BASE_URL + "/api/catalog/v1/book/isbn/" + URLEncoder.encode(isbn, StandardCharsets.UTF_8));
        try {
            log.info("Bookshelf API URL: {}", uri);
            HttpResponse<String> response = httpClient.send(
                    authorizedRequestBuilder(uri).GET().build(),
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() == 404) {
                return null;
            }
            if (response.statusCode() != 200) {
                log.warn("Bookshelf ISBN lookup failed. Status: {}, Response: {}", response.statusCode(), response.body());
                return null;
            }

            BookshelfBookResponse bookResponse = objectMapper.readValue(response.body(), BookshelfBookResponse.class);
            return bookResponse == null ? null : map(bookResponse);
        } catch (IOException e) {
            log.error("IO error while fetching metadata from Bookshelf ISBN lookup: {}", e.getMessage());
            return null;
        } catch (InterruptedException e) {
            log.error("Request to Bookshelf ISBN lookup was interrupted");
            Thread.currentThread().interrupt();
            return null;
        }
    }

    private List<BookMetadata> fetchBySearch(Book book, FetchMetadataRequest request) {
        String query = buildQuery(book, request);
        if (query == null || query.isBlank()) {
            return List.of();
        }

        URI uri = UriComponentsBuilder.fromUriString(BASE_URL + "/api/catalog/v1/search")
                .queryParam("q", query)
                .queryParam("limit", SEARCH_LIMIT)
                .build()
                .toUri();

        try {
            log.info("Bookshelf API URL: {}", uri);
            HttpResponse<String> response = httpClient.send(
                    authorizedRequestBuilder(uri).GET().build(),
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() != 200) {
                log.warn("Bookshelf search request failed. Status: {}, Response: {}", response.statusCode(), response.body());
                return List.of();
            }

            BookshelfSearchResponse searchResponse = objectMapper.readValue(response.body(), BookshelfSearchResponse.class);
            if (searchResponse == null || searchResponse.getBooks() == null) {
                return List.of();
            }

            return searchResponse.getBooks().stream()
                    .map(this::map)
                    .filter(metadata -> metadata.getTitle() != null && !metadata.getTitle().isBlank())
                    .toList();
        } catch (IOException e) {
            log.error("IO error while fetching metadata from Bookshelf search API: {}", e.getMessage());
            return List.of();
        } catch (InterruptedException e) {
            log.error("Request to Bookshelf search API was interrupted");
            Thread.currentThread().interrupt();
            return List.of();
        }
    }

    private String buildQuery(Book book, FetchMetadataRequest request) {
        String title = Optional.ofNullable(request.getTitle())
                .filter(value -> !value.isBlank())
                .orElseGet(() -> Optional.ofNullable(book.getPrimaryFile())
                        .map(BookFile::getFileName)
                        .filter(fileName -> !fileName.isBlank())
                        .map(BookUtils::cleanFileName)
                        .orElse(null));

        if (title == null || title.isBlank()) {
            return null;
        }

        String author = request.getAuthor();
        return (author != null && !author.isBlank()) ? title + " " + author : title;
    }

    private BookMetadata map(BookshelfBookResponse response) {
        String rawIsbn = response.getIsbn();
        String isbn13 = isbn13(rawIsbn);
        String isbn10 = isbn10(rawIsbn);
        // For Kindle-exclusive/ASIN-only titles, Bookshelf puts the ASIN in the "isbn" field
        // and leaves amazon_asin null; only treat it as an ASIN if it isn't a real ISBN.
        String asinFromIsbnField = (isbn13 == null && isbn10 == null) ? asinShaped(rawIsbn) : null;

        return BookMetadata.builder()
                .provider(MetadataProvider.Bookshelf)
                .title(cleanString(response.getTitle()))
                .authors(cleanAuthors(response.getAuthor()))
                .description(cleanString(response.getDescription()))
                .thumbnailUrl(resolveCoverUrl(response.getCoverUrl()))
                .publishedDate(parseDate(response.getPublishedDate()))
                .pageCount(response.getPageCount())
                .publisher(cleanString(response.getPublisher()))
                .categories(cleanCategories(response.getGenre(), response.getSubGenre()))
                .language(cleanString(response.getLanguage()))
                .seriesName(cleanString(response.getSeriesName()))
                .seriesNumber(parseSeriesNumber(response.getSeriesNumber()))
                .isbn13(isbn13)
                .isbn10(isbn10)
                .asin(firstNonBlank(cleanString(response.getAmazonAsin()), asinFromIsbnField))
                .build();
    }

    private String asinShaped(String rawIsbn) {
        if (rawIsbn == null) {
            return null;
        }
        String trimmed = rawIsbn.trim().toUpperCase();
        return ASIN_PATTERN.matcher(trimmed).matches() ? trimmed : null;
    }

    private Float parseSeriesNumber(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        try {
            return Float.parseFloat(rawValue.trim());
        } catch (NumberFormatException e) {
            log.debug("Could not parse Bookshelf series_number '{}' as a float", rawValue);
            return null;
        }
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    private List<String> cleanAuthors(String author) {
        if (author == null || author.isBlank()) {
            return List.of();
        }
        return List.of(author.split(",")).stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }

    private Set<String> cleanCategories(String genre, String subGenre) {
        Set<String> categories = new LinkedHashSet<>();
        for (String value : new String[]{genre, subGenre}) {
            if (value != null && !value.isBlank() && !"uncategorised".equalsIgnoreCase(value.trim())) {
                categories.add(value.trim());
            }
        }
        return categories;
    }

    private String resolveCoverUrl(String coverUrl) {
        if (coverUrl == null || coverUrl.isBlank()) {
            return null;
        }
        if (coverUrl.startsWith("http://") || coverUrl.startsWith("https://")) {
            return coverUrl;
        }
        return BASE_URL + (coverUrl.startsWith("/") ? coverUrl : "/" + coverUrl);
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            var matcher = YEAR_PATTERN.matcher(value.trim());
            if (matcher.find()) {
                try {
                    return LocalDate.of(Integer.parseInt(matcher.group(1)), 1, 1);
                } catch (NumberFormatException | DateTimeParseException ignored) {
                    return null;
                }
            }
            return null;
        }
    }

    private String isbn13(String isbn) {
        String cleaned = ParserUtils.cleanIsbn(isbn);
        return cleaned != null && cleaned.length() == 13 ? cleaned : null;
    }

    private String isbn10(String isbn) {
        String cleaned = ParserUtils.cleanIsbn(isbn);
        return cleaned != null && cleaned.length() == 10 ? cleaned : null;
    }

    private String cleanString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return WHITESPACE_PATTERN.matcher(value.trim()).replaceAll(" ");
    }
}
