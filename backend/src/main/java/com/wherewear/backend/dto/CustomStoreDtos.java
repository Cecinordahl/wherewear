package com.wherewear.backend.dto;

import jakarta.validation.constraints.NotBlank;

public class CustomStoreDtos {

    private CustomStoreDtos() {
    }

    public record CustomStoreRequest(
            @NotBlank String name,
            String url
    ) {
    }

    public record CustomStoreResponse(
            String id,
            String name,
            String url
    ) {
    }
}
