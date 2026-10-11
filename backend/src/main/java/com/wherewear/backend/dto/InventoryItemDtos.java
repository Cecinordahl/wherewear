package com.wherewear.backend.dto;

import jakarta.validation.constraints.NotBlank;

public class InventoryItemDtos {

    private InventoryItemDtos() {
    }

    public record InventoryItemRequest(
            @NotBlank String locationId,
            @NotBlank String category,
            @NotBlank String name,
            String brand,
            // Optional compressed "data:image/jpeg;base64,..." photo, sent by
            // bulk photo import (the browser resizes the photo before upload).
            String photoDataUrl
    ) {
    }

    public record InventoryItemUpdateRequest(
            @NotBlank String category,
            @NotBlank String name,
            String brand
    ) {
    }

    public record SetPhotoRequest(
            @NotBlank String sourceImageUrl
    ) {
    }

    // The user's own photo, resized in the browser. null clears the photo.
    public record SetPhotoDataRequest(
            String photoDataUrl
    ) {
    }

    public record InventoryItemResponse(
            String id,
            String locationId,
            String category,
            String name,
            String brand,
            String photoDataUrl
    ) {
    }
}
