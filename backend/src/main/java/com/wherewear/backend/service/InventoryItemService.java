package com.wherewear.backend.service;

import com.wherewear.backend.dto.InventoryItemDtos.InventoryItemRequest;
import com.wherewear.backend.dto.InventoryItemDtos.InventoryItemResponse;
import com.wherewear.backend.dto.InventoryItemDtos.InventoryItemUpdateRequest;
import com.wherewear.backend.model.InventoryItem;
import com.wherewear.backend.productlookup.ProductLookupService;
import com.wherewear.backend.repository.InventoryItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Base64;
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class InventoryItemService {

    private static final String JPEG_DATA_URL_PREFIX = "data:image/jpeg;base64,";
    // Keeps the item doc well under Firestore's 1 MiB document limit.
    private static final int MAX_PHOTO_DATA_URL_LENGTH = 700_000;

    private final InventoryItemRepository inventoryItemRepository;
    private final ProductLookupService productLookupService;

    public InventoryItemService(InventoryItemRepository inventoryItemRepository, ProductLookupService productLookupService) {
        this.inventoryItemRepository = inventoryItemRepository;
        this.productLookupService = productLookupService;
    }

    public List<InventoryItemResponse> listForLocation(String userId, String locationId) {
        return inventoryItemRepository.findByLocation(userId, locationId).stream()
                .map(InventoryItemService::toResponse).toList();
    }

    public InventoryItemResponse create(String userId, InventoryItemRequest request) {
        InventoryItem item = new InventoryItem();
        item.setUserId(userId);
        item.setLocationId(request.locationId());
        item.setCategory(request.category());
        item.setName(request.name());
        item.setBrand(blankToNull(request.brand()));
        item.setPhotoDataUrl(validatePhotoDataUrl(request.photoDataUrl()));
        return toResponse(inventoryItemRepository.save(item));
    }

    public InventoryItemResponse update(String userId, String itemId, InventoryItemUpdateRequest request) {
        InventoryItem existing = requireOwned(userId, itemId);
        existing.setCategory(request.category());
        existing.setName(request.name());
        existing.setBrand(blankToNull(request.brand()));
        existing.setUpdatedAt(null);
        return toResponse(inventoryItemRepository.save(existing));
    }

    public void delete(String userId, String itemId) {
        requireOwned(userId, itemId);
        inventoryItemRepository.deleteById(itemId);
    }

    public InventoryItemResponse setPhoto(String userId, String itemId, String sourceImageUrl) {
        InventoryItem existing = requireOwned(userId, itemId);
        existing.setPhotoDataUrl(productLookupService.resolvePhotoDataUrl(sourceImageUrl));
        existing.setUpdatedAt(null);
        return toResponse(inventoryItemRepository.save(existing));
    }

    private InventoryItem requireOwned(String userId, String itemId) {
        InventoryItem item = inventoryItemRepository.findById(itemId);
        if (item == null) {
            throw new ResponseStatusException(NOT_FOUND, "Item not found");
        }
        if (!item.getUserId().equals(userId)) {
            throw new ResponseStatusException(FORBIDDEN, "Not your item");
        }
        return item;
    }

    private static InventoryItemResponse toResponse(InventoryItem item) {
        return new InventoryItemResponse(
                item.getId(), item.getLocationId(), item.getCategory(), item.getName(), item.getBrand(), item.getPhotoDataUrl());
    }

    static String validatePhotoDataUrl(String photoDataUrl) {
        if (photoDataUrl == null || photoDataUrl.isBlank()) {
            return null;
        }
        if (!photoDataUrl.startsWith(JPEG_DATA_URL_PREFIX)) {
            throw new ResponseStatusException(BAD_REQUEST, "Photo must be a JPEG data URL");
        }
        if (photoDataUrl.length() > MAX_PHOTO_DATA_URL_LENGTH) {
            throw new ResponseStatusException(BAD_REQUEST, "Photo is too large");
        }
        try {
            Base64.getDecoder().decode(photoDataUrl.substring(JPEG_DATA_URL_PREFIX.length()));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(BAD_REQUEST, "Photo isn't valid base64", e);
        }
        return photoDataUrl;
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
