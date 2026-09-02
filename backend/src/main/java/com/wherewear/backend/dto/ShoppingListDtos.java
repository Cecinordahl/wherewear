package com.wherewear.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class ShoppingListDtos {

    private ShoppingListDtos() {
    }

    /** Fixed pseudo-locations, not real Location documents. */
    public static final String HOME_LOCATION_ID = "HOME";
    public static final String ONLINE_LOCATION_ID = "ONLINE";

    public record ShoppingListItemRequest(
            @NotBlank String name,
            String locationId,
            String tripDate,
            @Min(0) Integer leadTimeDays,
            String storeName,
            String storeUrl,
            String productUrl
    ) {
    }

    public record ShoppingListItemResponse(
            String id,
            String name,
            String locationId,
            boolean checked,
            String tripDate,
            Integer leadTimeDays,
            String orderByDate,
            boolean dueSoon,
            boolean needsDate,
            String storeName,
            String storeUrl,
            String productUrl
    ) {
    }
}
