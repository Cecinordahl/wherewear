package com.wherewear.backend.service;

import com.wherewear.backend.dto.PackingListDtos.PackingCategoryDto;
import com.wherewear.backend.dto.PackingListDtos.PackingItemDto;
import com.wherewear.backend.dto.PackingListDtos.PackingListResponse;
import com.wherewear.backend.dto.PackingListDtos.SavePackingListRequest;
import com.wherewear.backend.model.*;
import com.wherewear.backend.repository.InventoryItemRepository;
import com.wherewear.backend.repository.LocationRepository;
import com.wherewear.backend.repository.PackingListRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class PackingListService {

    private final PackingListRepository packingListRepository;
    private final LocationRepository locationRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final CategoryTemplateService categoryTemplateService;

    public PackingListService(
            PackingListRepository packingListRepository,
            LocationRepository locationRepository,
            InventoryItemRepository inventoryItemRepository,
            CategoryTemplateService categoryTemplateService
    ) {
        this.packingListRepository = packingListRepository;
        this.locationRepository = locationRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.categoryTemplateService = categoryTemplateService;
    }

    /** Returns the saved list, generating (and saving) it on first access. */
    public PackingListResponse getOrGenerate(String userId, String locationId, Season season) {
        String id = PackingListRepository.idFor(userId, locationId, season);
        PackingList existing = packingListRepository.findById(id);
        if (existing != null) {
            return toResponse(existing);
        }
        return toResponse(generateAndSave(userId, locationId, season));
    }

    /** Explicitly re-generates from templates + inventory, discarding edits. */
    public PackingListResponse reset(String userId, String locationId, Season season) {
        return toResponse(generateAndSave(userId, locationId, season));
    }

    public PackingListResponse save(String userId, String locationId, Season season, SavePackingListRequest request) {
        Location location = requireOwnedLocation(userId, locationId);
        String id = PackingListRepository.idFor(userId, location.getId(), season);
        PackingList packingList = packingListRepository.findById(id);
        if (packingList == null) {
            packingList = new PackingList();
            packingList.setId(id);
            packingList.setUserId(userId);
            packingList.setLocationId(location.getId());
            packingList.setSeason(season);
        }
        packingList.setCategories(request.categories().stream()
                .map(PackingListService::fromDto)
                .toList());
        packingList.setUpdatedAt(null);
        packingListRepository.save(packingList);
        return toResponse(packingList);
    }

    private PackingList generateAndSave(String userId, String locationId, Season season) {
        Location location = requireOwnedLocation(userId, locationId);
        List<InventoryItem> inventory = inventoryItemRepository.findByLocation(userId, location.getId());

        List<PackingCategory> categories = new ArrayList<>();
        for (var template : categoryTemplateService.listForLocationType(userId, location.getType())) {
            List<PackingItem> items = new ArrayList<>();
            for (String templateItemName : template.items()) {
                InventoryItem match = findMatch(templateItemName, inventory);
                items.add(new PackingItem(templateItemName, match != null, match != null ? match.getId() : null));
            }
            categories.add(new PackingCategory(template.category(), items));
        }

        PackingList packingList = new PackingList();
        packingList.setId(PackingListRepository.idFor(userId, location.getId(), season));
        packingList.setUserId(userId);
        packingList.setLocationId(location.getId());
        packingList.setSeason(season);
        packingList.setCategories(categories);
        packingListRepository.save(packingList);
        return packingList;
    }

    private static InventoryItem findMatch(String templateItemName, List<InventoryItem> inventory) {
        for (InventoryItem item : inventory) {
            if (ItemNameMatcher.matches(templateItemName, item.getName())) {
                return item;
            }
        }
        return null;
    }

    private Location requireOwnedLocation(String userId, String locationId) {
        Location location = locationRepository.findById(locationId);
        if (location == null) {
            throw new ResponseStatusException(NOT_FOUND, "Location not found");
        }
        if (!location.getUserId().equals(userId)) {
            throw new ResponseStatusException(FORBIDDEN, "Not your location");
        }
        return location;
    }

    private static PackingCategory fromDto(PackingCategoryDto dto) {
        List<PackingItem> items = dto.items().stream()
                .map(i -> new PackingItem(i.name(), i.checked(), null))
                .toList();
        return new PackingCategory(dto.category(), items);
    }

    private static PackingListResponse toResponse(PackingList packingList) {
        List<PackingCategoryDto> categories = packingList.getCategories().stream()
                .map(c -> new PackingCategoryDto(
                        c.getCategory(),
                        c.getItems().stream()
                                .map(i -> new PackingItemDto(i.getName(), i.isChecked()))
                                .toList()
                ))
                .toList();
        return new PackingListResponse(packingList.getLocationId(), packingList.getSeason().name(), categories);
    }
}
