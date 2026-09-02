package com.wherewear.backend.controller;

import com.wherewear.backend.dto.CategoryTemplateDtos.CategoryTemplateResponse;
import com.wherewear.backend.dto.CategoryTemplateDtos.UpdateItemsRequest;
import com.wherewear.backend.model.LocationType;
import com.wherewear.backend.security.RequestUserContext;
import com.wherewear.backend.service.CategoryTemplateService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/category-templates")
public class CategoryTemplateController {

    private final CategoryTemplateService categoryTemplateService;

    public CategoryTemplateController(CategoryTemplateService categoryTemplateService) {
        this.categoryTemplateService = categoryTemplateService;
    }

    @GetMapping
    public List<CategoryTemplateResponse> list(@RequestParam LocationType locationType) {
        return categoryTemplateService.listForLocationType(RequestUserContext.requireUserId(), locationType);
    }

    // category is a query param, not a path variable: several category names
    // (e.g. "Golf/Tennis", "Div/Tech") contain a literal "/" which would
    // otherwise collide with the URL path separator.
    @PutMapping
    public CategoryTemplateResponse updateItems(
            @RequestParam LocationType locationType,
            @RequestParam String category,
            @Valid @RequestBody UpdateItemsRequest request
    ) {
        return categoryTemplateService.updateItems(RequestUserContext.requireUserId(), locationType, category, request);
    }
}
