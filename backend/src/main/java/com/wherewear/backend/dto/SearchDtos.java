package com.wherewear.backend.dto;

public class SearchDtos {

    private SearchDtos() {
    }

    public record SearchResult(
            String itemId,
            String itemName,
            String category,
            String locationId,
            String locationName
    ) {
    }
}
