package com.adityachandel.booklore.util.kobo;

import com.adityachandel.booklore.util.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@Slf4j
public class KoboUrlBuilder {

    public UriComponentsBuilder baseBuilder() {
        HttpServletRequest request = RequestUtils.getCurrentRequest();

        UriComponentsBuilder builder = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .replacePath("")
                .replaceQuery(null)
                .port(-1); // drop default port

        String host = builder.build().getHost();
        String scheme = builder.build().getScheme();

        if (host == null) host = "";

        String xfPort = request.getHeader("X-Forwarded-Port");
        try {
            int port = Integer.parseInt(xfPort);

            if (host.matches("\\d+\\.\\d+\\.\\d+\\.\\d+") || "localhost".equals(host)) {
                builder.port(port);
            }
            log.info("Applied X-Forwarded-Port: {}", port);
        } catch (NumberFormatException e) {
            log.warn("Invalid X-Forwarded-Port header: {}", xfPort);
        }

        log.info("Final base URL: {}", builder.build().toUriString());
        return builder;
    }

    public String downloadUrl(String token, Long bookId) {
        return baseBuilder()
                .pathSegment("api", "kobo", token, "v1", "books", "{bookId}", "download")
                .buildAndExpand(bookId)
                .toUriString();
    }

    public String imageUrlTemplate(String token) {
        return baseBuilder()
                .pathSegment("api", "kobo", token, "v1", "books", "{ImageId}", "thumbnail", "{Width}", "{Height}", "false", "image.jpg")
                .build()
                .toUriString();
    }

    public String imageUrlQualityTemplate(String token) {
        return baseBuilder()
                .pathSegment("api", "kobo", token, "v1", "books", "{ImageId}", "thumbnail", "{Width}", "{Height}", "{Quality}", "{IsGreyscale}", "image.jpg")
                .build()
                .toUriString();
    }
}