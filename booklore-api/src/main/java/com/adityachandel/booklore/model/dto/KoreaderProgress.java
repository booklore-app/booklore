package com.adityachandel.booklore.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;

@Data
@Builder
@ToString
public class KoreaderProgress {

    @NotBlank(message = "Document (book hash) must not be blank")
    private String document;

    @NotNull(message = "Percentage must not be null")
    private Float percentage;

    @NotBlank(message = "Progress must not be blank")
    private String progress;

    private String device;
    private String device_id;
}
