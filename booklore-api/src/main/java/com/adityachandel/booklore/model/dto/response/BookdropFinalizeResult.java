package com.adityachandel.booklore.model.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class BookdropFinalizeResult {
    @Builder.Default
    private List<BookdropFileResult> results = new ArrayList<>();
}
