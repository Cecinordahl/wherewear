package com.wherewear.backend.controller;

import com.wherewear.backend.dto.ReceiptImportDtos.ReceiptItemCandidate;
import com.wherewear.backend.model.LocationType;
import com.wherewear.backend.receiptimport.ReceiptImportService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** Extracts candidate inventory items from a photo of a receipt (via Gemini). See README. */
@RestController
@RequestMapping("/api/receipt-import")
public class ReceiptImportController {

    private final ReceiptImportService receiptImportService;

    public ReceiptImportController(ReceiptImportService receiptImportService) {
        this.receiptImportService = receiptImportService;
    }

    @PostMapping(consumes = "multipart/form-data")
    public List<ReceiptItemCandidate> importReceipt(
            @RequestParam("file") MultipartFile file,
            @RequestParam LocationType locationType
    ) {
        return receiptImportService.extractItems(file, locationType);
    }
}
