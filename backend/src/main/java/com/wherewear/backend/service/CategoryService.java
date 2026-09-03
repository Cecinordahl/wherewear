package com.wherewear.backend.service;

import com.wherewear.backend.catalog.CategoryCatalog;
import com.wherewear.backend.model.CustomCategory;
import com.wherewear.backend.model.LocationType;
import com.wherewear.backend.repository.CustomCategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
public class CategoryService {

    private final CustomCategoryRepository customCategoryRepository;

    public CategoryService(CustomCategoryRepository customCategoryRepository) {
        this.customCategoryRepository = customCategoryRepository;
    }

    public List<String> listForLocationType(String userId, LocationType locationType) {
        List<String> categories = new ArrayList<>(CategoryCatalog.categoriesFor(locationType));
        for (CustomCategory custom : customCategoryRepository.findByLocationType(userId, locationType)) {
            categories.add(custom.getName());
        }
        return categories;
    }

    public List<String> addCustomCategory(String userId, LocationType locationType, String rawName) {
        String name = rawName.trim();
        List<String> existing = listForLocationType(userId, locationType);
        if (existing.stream().anyMatch(c -> c.equalsIgnoreCase(name))) {
            throw new ResponseStatusException(BAD_REQUEST, "Kategorien «" + name + "» finnes allerede");
        }

        CustomCategory category = new CustomCategory();
        category.setUserId(userId);
        category.setLocationType(locationType);
        category.setName(name);
        customCategoryRepository.save(category);

        existing.add(name);
        return existing;
    }
}
