package com.wherewear.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public class PackingListDtos {

    private PackingListDtos() {
    }

    private static final String ISO_DATE = "\\d{4}-\\d{2}-\\d{2}";

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

    public record PackingTripResponse(
            String id,
            String locationId,
            String season,
            String tripDate,
            boolean past,
            List<PackingCategoryDto> categories
    ) {
    }

    /** Upcoming/undated trips first (soonest first), then historic ones (newest first). */
    public record PackingTripsResponse(
            List<PackingTripResponse> active,
            List<PackingTripResponse> past
    ) {
    }

    public record CreatePackingTripRequest(
            @Pattern(regexp = ISO_DATE) String tripDate
    ) {
    }

    public record SavePackingTripRequest(
            @Pattern(regexp = ISO_DATE) String tripDate,
            @NotNull @Valid List<PackingCategoryDto> categories
    ) {
    }
}
