package com.wherewear.backend.dto;

public class ReceiptImportDtos {

    private ReceiptImportDtos() {
    }

    public record ReceiptItemCandidate(
            String name,
            String category
    ) {
    }
}
