package com.wherewear.backend.dto;

import jakarta.validation.constraints.NotBlank;

public class ProductLookupDtos {

    private ProductLookupDtos() {
    }

    public record TextSearchRequest(
            @NotBlank String query
    ) {
    }

    public record ProductCandidate(
            String title,
            String source,
            String pageUrl,
            String imageUrl
    ) {
    }
}
