package com.adityachandel.booklore.controller;

import com.adityachandel.booklore.model.dto.Series;
import com.adityachandel.booklore.service.SeriesService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/series")
@AllArgsConstructor
public class SeriesController {

    private final SeriesService seriesService;

    @GetMapping
    public ResponseEntity<List<Series>> getSeries() {
        return ResponseEntity.ok(seriesService.getAllSeries());
    }
}