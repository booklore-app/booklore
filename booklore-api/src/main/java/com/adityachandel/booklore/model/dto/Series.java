package com.adityachandel.booklore.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Series {
    private String name;
    private long bookCount;
}