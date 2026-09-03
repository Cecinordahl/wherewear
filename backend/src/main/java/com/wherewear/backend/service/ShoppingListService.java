package com.wherewear.backend.service;

import com.wherewear.backend.dto.ShoppingListDtos.ShoppingListItemRequest;
import com.wherewear.backend.dto.ShoppingListDtos.ShoppingListItemResponse;
import com.wherewear.backend.model.ShoppingListItem;
import com.wherewear.backend.repository.ShoppingListItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

import static com.wherewear.backend.dto.ShoppingListDtos.ONLINE_LOCATION_ID;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ShoppingListService {

    private final ShoppingListItemRepository shoppingListItemRepository;

    public ShoppingListService(ShoppingListItemRepository shoppingListItemRepository) {
        this.shoppingListItemRepository = shoppingListItemRepository;
    }

    public List<ShoppingListItemResponse> listForUser(String userId) {
        return shoppingListItemRepository.findAllForUser(userId).stream()
                .map(ShoppingListService::toResponse)
                .toList();
    }

    public ShoppingListItemResponse create(String userId, ShoppingListItemRequest request) {
        ShoppingListItem item = new ShoppingListItem();
        item.setUserId(userId);
        applyRequest(item, request);
        return toResponse(shoppingListItemRepository.save(item));
    }

    public ShoppingListItemResponse update(String userId, String itemId, ShoppingListItemRequest request) {
        ShoppingListItem existing = requireOwned(userId, itemId);
        applyRequest(existing, request);
        existing.setUpdatedAt(null);
        return toResponse(shoppingListItemRepository.save(existing));
    }

    public ShoppingListItemResponse setChecked(String userId, String itemId, boolean checked) {
        ShoppingListItem existing = requireOwned(userId, itemId);
        existing.setChecked(checked);
        existing.setUpdatedAt(null);
        return toResponse(shoppingListItemRepository.save(existing));
    }

    public void delete(String userId, String itemId) {
        requireOwned(userId, itemId);
        shoppingListItemRepository.deleteById(itemId);
    }

    private static void applyRequest(ShoppingListItem item, ShoppingListItemRequest request) {
        item.setName(request.name());
        item.setLocationId(request.locationId());
        item.setNeededForLocationId(blankToNull(request.neededForLocationId()));
        boolean online = ONLINE_LOCATION_ID.equals(request.locationId());
        // A date/lead-time only means anything for an online order.
        item.setTripDate(online ? request.tripDate() : null);
        item.setLeadTimeDays(online ? request.leadTimeDays() : null);
        item.setStoreName(blankToNull(request.storeName()));
        item.setStoreUrl(blankToNull(request.storeUrl()));
        item.setProductUrl(blankToNull(request.productUrl()));
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    private ShoppingListItem requireOwned(String userId, String itemId) {
        ShoppingListItem item = shoppingListItemRepository.findById(itemId);
        if (item == null) {
            throw new ResponseStatusException(NOT_FOUND, "Shopping list item not found");
        }
        if (!item.getUserId().equals(userId)) {
            throw new ResponseStatusException(FORBIDDEN, "Not your shopping list item");
        }
        return item;
    }

    private static ShoppingListItemResponse toResponse(ShoppingListItem item) {
        boolean online = ONLINE_LOCATION_ID.equals(item.getLocationId());
        LocalDate orderByDate = computeOrderByDate(item.getTripDate(), item.getLeadTimeDays());
        boolean dueSoon = !item.isChecked() && orderByDate != null && !orderByDate.isAfter(LocalDate.now());
        boolean needsDate = !item.isChecked() && online && item.getTripDate() == null;
        return new ShoppingListItemResponse(
                item.getId(),
                item.getName(),
                item.getLocationId(),
                item.getNeededForLocationId(),
                item.isChecked(),
                item.getTripDate(),
                item.getLeadTimeDays(),
                orderByDate != null ? orderByDate.toString() : null,
                dueSoon,
                needsDate,
                item.getStoreName(),
                item.getStoreUrl(),
                item.getProductUrl()
        );
    }

    private static LocalDate computeOrderByDate(String tripDate, Integer leadTimeDays) {
        if (tripDate == null || leadTimeDays == null) {
            return null;
        }
        return LocalDate.parse(tripDate).minusDays(leadTimeDays);
    }
}
