package com.adityachandel.booklore.model.dto.request;

import lombok.Data;

import java.util.Set;

@Data
public class FileMoveRequest {
    private Set<Long> bookIds;
}
