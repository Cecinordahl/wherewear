package com.wherewear.backend.controller;

import com.wherewear.backend.catalog.CategoryCatalog;
import com.wherewear.backend.model.LocationType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Exposes the fixed category catalog per location type (see CategoryCatalog). */
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    @GetMapping
    public List<String> categoriesForLocationType(@RequestParam LocationType locationType) {
        return CategoryCatalog.categoriesFor(locationType);
    }
}
