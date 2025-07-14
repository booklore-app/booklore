package com.adityachandel.booklore.model.dto.request;

import java.util.List;

public record ReadStatusUpdateRequest(List<Long> ids, String status) {
}
