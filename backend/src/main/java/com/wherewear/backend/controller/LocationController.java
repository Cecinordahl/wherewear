package com.wherewear.backend.controller;

import com.wherewear.backend.dto.LocationDtos.LocationRequest;
import com.wherewear.backend.dto.LocationDtos.LocationResponse;
import com.wherewear.backend.security.RequestUserContext;
import com.wherewear.backend.service.LocationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/locations")
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping
    public List<LocationResponse> list() {
        return locationService.listForUser(RequestUserContext.requireUserId());
    }

    @PostMapping
    public LocationResponse create(@Valid @RequestBody LocationRequest request) {
        return locationService.create(RequestUserContext.requireUserId(), request);
    }

    @PutMapping("/{id}")
    public LocationResponse update(@PathVariable String id, @Valid @RequestBody LocationRequest request) {
        return locationService.update(RequestUserContext.requireUserId(), id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        locationService.delete(RequestUserContext.requireUserId(), id);
    }
}
