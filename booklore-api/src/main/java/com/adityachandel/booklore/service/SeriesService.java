package com.adityachandel.booklore.service;

import com.adityachandel.booklore.model.dto.Series;
import com.adityachandel.booklore.repository.BookMetadataRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class SeriesService {

    private final BookMetadataRepository bookMetadataRepository;

    public List<Series> getAllSeries() {
        return bookMetadataRepository.findAllSeriesWithBookCount();
    }
}