package com.wherewear.backend.dto;

import jakarta.validation.constraints.NotBlank;

public class InventoryItemDtos {

    private InventoryItemDtos() {
    }

    public record InventoryItemRequest(
            @NotBlank String locationId,
            @NotBlank String category,
            @NotBlank String name
    ) {
    }

    public record InventoryItemUpdateRequest(
            @NotBlank String category,
            @NotBlank String name
    ) {
    }

    public record SetPhotoRequest(
            @NotBlank String sourceImageUrl
    ) {
    }

    public record InventoryItemResponse(
            String id,
            String locationId,
            String category,
            String name,
            String photoDataUrl
    ) {
    }
}
