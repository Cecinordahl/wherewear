package com.wherewear.backend.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public class CategoryTemplateDtos {

    private CategoryTemplateDtos() {
    }

    public record CategoryTemplateResponse(
            String category,
            List<String> items
    ) {
    }

    public record UpdateItemsRequest(
            @NotNull List<String> items
    ) {
    }
}
