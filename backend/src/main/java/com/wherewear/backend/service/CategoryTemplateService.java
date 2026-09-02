package com.wherewear.backend.service;

import com.wherewear.backend.catalog.CategoryCatalog;
import com.wherewear.backend.dto.CategoryTemplateDtos.CategoryTemplateResponse;
import com.wherewear.backend.dto.CategoryTemplateDtos.UpdateItemsRequest;
import com.wherewear.backend.model.CategoryTemplate;
import com.wherewear.backend.model.LocationType;
import com.wherewear.backend.model.TemplateItem;
import com.wherewear.backend.repository.CategoryTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
public class CategoryTemplateService {

    private final CategoryTemplateRepository categoryTemplateRepository;

    public CategoryTemplateService(CategoryTemplateRepository categoryTemplateRepository) {
        this.categoryTemplateRepository = categoryTemplateRepository;
    }

    /** Called once per new user (see UserService) to create their starter templates. */
    public void seedDefaultsForNewUser(String userId) {
        for (LocationType type : LocationType.values()) {
            for (String category : CategoryCatalog.categoriesFor(type)) {
                CategoryTemplate template = new CategoryTemplate();
                template.setId(CategoryTemplateRepository.idFor(userId, type, category));
                template.setUserId(userId);
                template.setLocationType(type);
                template.setCategory(category);
                template.setItems(toTemplateItems(CategoryCatalog.seedItemsFor(type, category)));
                categoryTemplateRepository.save(template);
            }
        }
    }

    public List<CategoryTemplateResponse> listForLocationType(String userId, LocationType locationType) {
        return categoryTemplateRepository.findByLocationType(userId, locationType).stream()
                .map(CategoryTemplateService::toResponse)
                .toList();
    }

    public CategoryTemplateResponse updateItems(String userId, LocationType locationType, String category, UpdateItemsRequest request) {
        if (!CategoryCatalog.categoriesFor(locationType).contains(category)) {
            throw new ResponseStatusException(BAD_REQUEST, "Unknown category '" + category + "' for " + locationType);
        }
        String id = CategoryTemplateRepository.idFor(userId, locationType, category);
        CategoryTemplate template = categoryTemplateRepository.findById(id);
        if (template == null) {
            template = new CategoryTemplate();
            template.setId(id);
            template.setUserId(userId);
            template.setLocationType(locationType);
            template.setCategory(category);
        }
        template.setItems(toTemplateItems(request.items()));
        template.setUpdatedAt(null);
        categoryTemplateRepository.save(template);
        return toResponse(template);
    }

    private static List<TemplateItem> toTemplateItems(List<String> names) {
        List<TemplateItem> items = new ArrayList<>();
        for (int i = 0; i < names.size(); i++) {
            items.add(new TemplateItem(names.get(i), i));
        }
        return items;
    }

    private static CategoryTemplateResponse toResponse(CategoryTemplate template) {
        List<String> names = template.getItems().stream()
                .sorted((a, b) -> Integer.compare(a.getOrder(), b.getOrder()))
                .map(TemplateItem::getName)
                .toList();
        return new CategoryTemplateResponse(template.getCategory(), names);
    }
}
