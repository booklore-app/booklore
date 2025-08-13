package com.adityachandel.booklore.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class Series {
    private String name;
    private long bookCount;
    private Long firstBookId;
    private Instant coverUpdatedOn;
}