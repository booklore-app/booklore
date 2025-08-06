package com.adityachandel.booklore.model.dto.progress;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.time.Instant;

@Data
@Builder
@ToString
public class KoreaderProgress {
    private String document;
    private Float percentage;
    private String progress;
    private String device;
    private String device_id;
}
