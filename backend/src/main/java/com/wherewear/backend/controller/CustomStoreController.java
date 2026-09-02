package com.wherewear.backend.controller;

import com.wherewear.backend.dto.CustomStoreDtos.CustomStoreRequest;
import com.wherewear.backend.dto.CustomStoreDtos.CustomStoreResponse;
import com.wherewear.backend.security.RequestUserContext;
import com.wherewear.backend.service.CustomStoreService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/custom-stores")
public class CustomStoreController {

    private final CustomStoreService customStoreService;

    public CustomStoreController(CustomStoreService customStoreService) {
        this.customStoreService = customStoreService;
    }

    @GetMapping
    public List<CustomStoreResponse> list() {
        return customStoreService.listForUser(RequestUserContext.requireUserId());
    }

    @PostMapping
    public CustomStoreResponse create(@Valid @RequestBody CustomStoreRequest request) {
        return customStoreService.create(RequestUserContext.requireUserId(), request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        customStoreService.delete(RequestUserContext.requireUserId(), id);
    }
}
