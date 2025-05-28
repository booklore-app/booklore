package com.adityachandel.booklore.service.metadata.parser.hardcover;

import lombok.Data;

@Data
public class GraphQLRequest {
    private String query;
    private String operationName;
}
