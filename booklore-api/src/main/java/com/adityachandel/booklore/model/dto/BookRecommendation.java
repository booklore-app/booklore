package com.adityachandel.booklore.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BookRecommendation {
    private Book book;
    private double similarityScore;
}
