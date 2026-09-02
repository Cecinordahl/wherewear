package com.wherewear.backend.controller;

import com.wherewear.backend.dto.ShoppingListDtos.ShoppingListItemRequest;
import com.wherewear.backend.dto.ShoppingListDtos.ShoppingListItemResponse;
import com.wherewear.backend.security.RequestUserContext;
import com.wherewear.backend.service.ShoppingListService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/shopping-list")
public class ShoppingListController {

    private final ShoppingListService shoppingListService;

    public ShoppingListController(ShoppingListService shoppingListService) {
        this.shoppingListService = shoppingListService;
    }

    @GetMapping
    public List<ShoppingListItemResponse> list() {
        return shoppingListService.listForUser(RequestUserContext.requireUserId());
    }

    @PostMapping
    public ShoppingListItemResponse create(@Valid @RequestBody ShoppingListItemRequest request) {
        return shoppingListService.create(RequestUserContext.requireUserId(), request);
    }

    @PutMapping("/{id}")
    public ShoppingListItemResponse update(@PathVariable String id, @Valid @RequestBody ShoppingListItemRequest request) {
        return shoppingListService.update(RequestUserContext.requireUserId(), id, request);
    }

    @PutMapping("/{id}/checked")
    public ShoppingListItemResponse setChecked(@PathVariable String id, @RequestBody Map<String, Boolean> body) {
        boolean checked = Boolean.TRUE.equals(body.get("checked"));
        return shoppingListService.setChecked(RequestUserContext.requireUserId(), id, checked);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        shoppingListService.delete(RequestUserContext.requireUserId(), id);
    }
}
