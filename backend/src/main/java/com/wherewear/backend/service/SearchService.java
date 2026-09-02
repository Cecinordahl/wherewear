package com.wherewear.backend.service;

import com.wherewear.backend.dto.SearchDtos.SearchResult;
import com.wherewear.backend.model.InventoryItem;
import com.wherewear.backend.model.Location;
import com.wherewear.backend.repository.InventoryItemRepository;
import com.wherewear.backend.repository.LocationRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SearchService {

    private final InventoryItemRepository inventoryItemRepository;
    private final LocationRepository locationRepository;

    public SearchService(InventoryItemRepository inventoryItemRepository, LocationRepository locationRepository) {
        this.inventoryItemRepository = inventoryItemRepository;
        this.locationRepository = locationRepository;
    }

    public List<SearchResult> search(String userId, String query) {
        String needle = query.toLowerCase().trim();
        if (needle.isEmpty()) {
            return List.of();
        }

        Map<String, String> locationNamesById = new HashMap<>();
        for (Location location : locationRepository.findAllForUser(userId)) {
            locationNamesById.put(location.getId(), location.getName());
        }

        return inventoryItemRepository.findAllForUser(userId).stream()
                .filter(item -> item.getName().toLowerCase().contains(needle))
                .map(item -> toResult(item, locationNamesById))
                .toList();
    }

    private static SearchResult toResult(InventoryItem item, Map<String, String> locationNamesById) {
        return new SearchResult(
                item.getId(),
                item.getName(),
                item.getCategory(),
                item.getLocationId(),
                locationNamesById.getOrDefault(item.getLocationId(), "Unknown location")
        );
    }
}
