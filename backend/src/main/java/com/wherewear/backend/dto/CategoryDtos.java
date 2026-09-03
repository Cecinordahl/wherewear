package com.wherewear.backend.dto;

import com.wherewear.backend.model.LocationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CategoryDtos {

    private CategoryDtos() {
    }

    public record AddCategoryRequest(
            @NotNull LocationType locationType,
            @NotBlank String name
    ) {
    }
}
