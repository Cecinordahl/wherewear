package com.wherewear.backend.controller;

import com.wherewear.backend.dto.PackingListDtos.CreatePackingTripRequest;
import com.wherewear.backend.dto.PackingListDtos.PackingTripResponse;
import com.wherewear.backend.dto.PackingListDtos.PackingTripsResponse;
import com.wherewear.backend.dto.PackingListDtos.SavePackingTripRequest;
import com.wherewear.backend.model.Season;
import com.wherewear.backend.security.RequestUserContext;
import com.wherewear.backend.service.PackingListService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/packing-lists")
public class PackingListController {

    private final PackingListService packingListService;

    public PackingListController(PackingListService packingListService) {
        this.packingListService = packingListService;
    }

    @GetMapping("/{locationId}/{season}/trips")
    public PackingTripsResponse listTrips(@PathVariable String locationId, @PathVariable Season season) {
        return packingListService.listTrips(RequestUserContext.requireUserId(), locationId, season);
    }

    @PostMapping("/{locationId}/{season}/trips")
    public PackingTripResponse createTrip(
            @PathVariable String locationId,
            @PathVariable Season season,
            @Valid @RequestBody CreatePackingTripRequest request
    ) {
        return packingListService.createTrip(RequestUserContext.requireUserId(), locationId, season, request.tripDate());
    }

    @PutMapping("/trips/{tripId}")
    public PackingTripResponse saveTrip(@PathVariable String tripId, @Valid @RequestBody SavePackingTripRequest request) {
        return packingListService.saveTrip(RequestUserContext.requireUserId(), tripId, request);
    }

    @PostMapping("/trips/{tripId}/reset")
    public PackingTripResponse resetTrip(@PathVariable String tripId) {
        return packingListService.resetTrip(RequestUserContext.requireUserId(), tripId);
    }

    @DeleteMapping("/trips/{tripId}")
    public void deleteTrip(@PathVariable String tripId) {
        packingListService.deleteTrip(RequestUserContext.requireUserId(), tripId);
    }
}
