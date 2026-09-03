package com.wherewear.backend.dto;

import com.wherewear.backend.model.LocationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class LocationDtos {

    private LocationDtos() {
    }

    public record LocationRequest(
            @NotBlank String name,
            @NotNull LocationType type,
            String icon
    ) {
    }

    public record LocationResponse(
            String id,
            String name,
            LocationType type,
            String icon
    ) {
    }
}
