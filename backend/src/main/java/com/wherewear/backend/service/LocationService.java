package com.wherewear.backend.service;

import com.wherewear.backend.dto.LocationDtos.LocationRequest;
import com.wherewear.backend.dto.LocationDtos.LocationResponse;
import com.wherewear.backend.model.Location;
import com.wherewear.backend.repository.LocationRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class LocationService {

    private final LocationRepository locationRepository;

    public LocationService(LocationRepository locationRepository) {
        this.locationRepository = locationRepository;
    }

    public List<LocationResponse> listForUser(String userId) {
        return locationRepository.findAllForUser(userId).stream().map(LocationService::toResponse).toList();
    }

    public LocationResponse create(String userId, LocationRequest request) {
        Location location = new Location();
        location.setUserId(userId);
        location.setName(request.name());
        location.setType(request.type());
        return toResponse(locationRepository.save(location));
    }

    public LocationResponse update(String userId, String locationId, LocationRequest request) {
        Location existing = requireOwned(userId, locationId);
        existing.setName(request.name());
        existing.setType(request.type());
        existing.setUpdatedAt(null); // let @ServerTimestamp refresh it
        return toResponse(locationRepository.save(existing));
    }

    public void delete(String userId, String locationId) {
        requireOwned(userId, locationId);
        locationRepository.deleteById(locationId);
    }

    private Location requireOwned(String userId, String locationId) {
        Location location = locationRepository.findById(locationId);
        if (location == null) {
            throw new ResponseStatusException(NOT_FOUND, "Location not found");
        }
        if (!location.getUserId().equals(userId)) {
            throw new ResponseStatusException(FORBIDDEN, "Not your location");
        }
        return location;
    }

    private static LocationResponse toResponse(Location location) {
        return new LocationResponse(location.getId(), location.getName(), location.getType());
    }
}
