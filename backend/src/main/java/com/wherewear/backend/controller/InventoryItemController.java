package com.wherewear.backend.controller;

import com.wherewear.backend.dto.InventoryItemDtos.InventoryItemRequest;
import com.wherewear.backend.dto.InventoryItemDtos.InventoryItemResponse;
import com.wherewear.backend.dto.InventoryItemDtos.InventoryItemUpdateRequest;
import com.wherewear.backend.security.RequestUserContext;
import com.wherewear.backend.service.InventoryItemService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class InventoryItemController {

    private final InventoryItemService inventoryItemService;

    public InventoryItemController(InventoryItemService inventoryItemService) {
        this.inventoryItemService = inventoryItemService;
    }

    @GetMapping("/locations/{locationId}/items")
    public List<InventoryItemResponse> listForLocation(@PathVariable String locationId) {
        return inventoryItemService.listForLocation(RequestUserContext.requireUserId(), locationId);
    }

    @PostMapping("/items")
    public InventoryItemResponse create(@Valid @RequestBody InventoryItemRequest request) {
        return inventoryItemService.create(RequestUserContext.requireUserId(), request);
    }

    @PutMapping("/items/{id}")
    public InventoryItemResponse update(@PathVariable String id, @Valid @RequestBody InventoryItemUpdateRequest request) {
        return inventoryItemService.update(RequestUserContext.requireUserId(), id, request);
    }

    @DeleteMapping("/items/{id}")
    public void delete(@PathVariable String id) {
        inventoryItemService.delete(RequestUserContext.requireUserId(), id);
    }
}
