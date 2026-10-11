package com.wherewear.backend.controller;

import com.wherewear.backend.dto.PhotoImportDtos.PhotoItemCandidate;
import com.wherewear.backend.model.LocationType;
import com.wherewear.backend.receiptimport.PhotoImportService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Identifies the clothing item in a single photo (via Gemini), for bulk photo import. See README. */
@RestController
@RequestMapping("/api/photo-import")
public class PhotoImportController {

    private final PhotoImportService photoImportService;

    public PhotoImportController(PhotoImportService photoImportService) {
        this.photoImportService = photoImportService;
    }

    @PostMapping(consumes = "multipart/form-data")
    public PhotoItemCandidate importPhoto(
            @RequestParam("file") MultipartFile file,
            @RequestParam LocationType locationType
    ) {
        return photoImportService.identifyItem(file, locationType);
    }
}
