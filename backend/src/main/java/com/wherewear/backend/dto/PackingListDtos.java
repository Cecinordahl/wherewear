package com.wherewear.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class PackingListDtos {

    private PackingListDtos() {
    }

    public record PackingItemDto(
            @NotBlank String name,
            boolean checked
    ) {
    }

    public record PackingCategoryDto(
            @NotBlank String category,
            @NotNull @Valid List<PackingItemDto> items
    ) {
    }

    public record PackingListResponse(
            String locationId,
            String season,
            List<PackingCategoryDto> categories
    ) {
    }

    public record SavePackingListRequest(
            @NotNull @Valid List<PackingCategoryDto> categories
    ) {
    }
}
