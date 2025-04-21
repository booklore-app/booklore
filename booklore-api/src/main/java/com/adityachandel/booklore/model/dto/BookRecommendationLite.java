package com.adityachandel.booklore.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BookRecommendationLite {
    private long b; // bookId
    private double s; // similarityScore
}
