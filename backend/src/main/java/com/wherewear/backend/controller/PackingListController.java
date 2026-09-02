package com.wherewear.backend.controller;

import com.wherewear.backend.dto.PackingListDtos.PackingListResponse;
import com.wherewear.backend.dto.PackingListDtos.SavePackingListRequest;
import com.wherewear.backend.model.Season;
import com.wherewear.backend.security.RequestUserContext;
import com.wherewear.backend.service.PackingListService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/packing-lists/{locationId}/{season}")
public class PackingListController {

    private final PackingListService packingListService;

    public PackingListController(PackingListService packingListService) {
        this.packingListService = packingListService;
    }

    @GetMapping
    public PackingListResponse get(@PathVariable String locationId, @PathVariable Season season) {
        return packingListService.getOrGenerate(RequestUserContext.requireUserId(), locationId, season);
    }

    @PutMapping
    public PackingListResponse save(
            @PathVariable String locationId,
            @PathVariable Season season,
            @Valid @RequestBody SavePackingListRequest request
    ) {
        return packingListService.save(RequestUserContext.requireUserId(), locationId, season, request);
    }

    @PostMapping("/reset")
    public PackingListResponse reset(@PathVariable String locationId, @PathVariable Season season) {
        return packingListService.reset(RequestUserContext.requireUserId(), locationId, season);
    }
}
