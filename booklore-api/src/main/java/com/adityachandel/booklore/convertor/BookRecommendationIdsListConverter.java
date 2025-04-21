package com.adityachandel.booklore.convertor;

import com.adityachandel.booklore.model.dto.BookRecommendationLite;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.List;

@Converter
public class BookRecommendationIdsListConverter implements AttributeConverter<List<BookRecommendationLite>, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public String convertToDatabaseColumn(List<BookRecommendationLite> recommendations) {
        try {
            return objectMapper.writeValueAsString(recommendations);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting BookRecommendation list to JSON", e);
        }
    }

    @Override
    public List<BookRecommendationLite> convertToEntityAttribute(String json) {
        if (json == null || json.trim().isEmpty()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<BookRecommendationLite>>() {
            });
        } catch (Exception e) {
            throw new RuntimeException("Error converting JSON to BookRecommendation list", e);
        }
    }
}
