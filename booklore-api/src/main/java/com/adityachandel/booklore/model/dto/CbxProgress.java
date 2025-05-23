package com.adityachandel.booklore.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class CbxProgress {
    @NotNull
    Integer page;
    @NotNull
    Float percentage;
}
