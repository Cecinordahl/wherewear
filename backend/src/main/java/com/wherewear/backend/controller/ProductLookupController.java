package com.wherewear.backend.controller;

import com.wherewear.backend.dto.ProductLookupDtos.ProductCandidate;
import com.wherewear.backend.dto.ProductLookupDtos.TextSearchRequest;
import com.wherewear.backend.productlookup.ProductLookupService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * "Find product photo" feature: search for a product by photo or by text
 * (via SerpAPI), so the user can pick a match and attach its photo to an
 * inventory item instead of taking their own. See README for setup.
 */
@RestController
@RequestMapping("/api/product-lookup")
public class ProductLookupController {

    private final ProductLookupService productLookupService;

    public ProductLookupController(ProductLookupService productLookupService) {
        this.productLookupService = productLookupService;
    }

    @PostMapping("/by-text")
    public List<ProductCandidate> byText(@Valid @RequestBody TextSearchRequest request) {
        return productLookupService.searchByText(request.query());
    }

    @PostMapping(value = "/by-photo", consumes = "multipart/form-data")
    public List<ProductCandidate> byPhoto(@RequestParam("file") MultipartFile file) {
        return productLookupService.searchByPhoto(file);
    }
}
