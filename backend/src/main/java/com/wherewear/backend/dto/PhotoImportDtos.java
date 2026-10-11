package com.wherewear.backend.dto;

public class PhotoImportDtos {

    private PhotoImportDtos() {
    }

    public record PhotoItemCandidate(
            String name,
            String category,
            String brand
    ) {
    }
}
