package com.wherewear.backend.controller;

import com.wherewear.backend.dto.CategoryDtos.AddCategoryRequest;
import com.wherewear.backend.model.LocationType;
import com.wherewear.backend.security.RequestUserContext;
import com.wherewear.backend.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Exposes the category list per location type: the fixed catalog (see CategoryCatalog) plus any the user has added themselves. */
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public List<String> categoriesForLocationType(@RequestParam LocationType locationType) {
        return categoryService.listForLocationType(RequestUserContext.requireUserId(), locationType);
    }

    @PostMapping
    public List<String> addCategory(@Valid @RequestBody AddCategoryRequest request) {
        return categoryService.addCustomCategory(RequestUserContext.requireUserId(), request.locationType(), request.name());
    }
}
