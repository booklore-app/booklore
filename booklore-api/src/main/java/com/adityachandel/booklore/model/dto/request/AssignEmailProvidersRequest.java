package com.adityachandel.booklore.model.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignEmailProvidersRequest {
    private List<Long> providerIds;
}
