package com.wherewear.backend.service;

import com.wherewear.backend.dto.PackingListDtos.PackingCategoryDto;
import com.wherewear.backend.dto.PackingListDtos.PackingItemDto;
import com.wherewear.backend.dto.PackingListDtos.PackingTripResponse;
import com.wherewear.backend.dto.PackingListDtos.PackingTripsResponse;
import com.wherewear.backend.dto.PackingListDtos.SavePackingTripRequest;
import com.wherewear.backend.model.*;
import com.wherewear.backend.repository.InventoryItemRepository;
import com.wherewear.backend.repository.LocationRepository;
import com.wherewear.backend.repository.PackingListRepository;
import com.wherewear.backend.repository.PackingTripRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class PackingListService {

    // The app's users are in Norway; a trip turns historic the day after its date there.
    private static final ZoneId ZONE = ZoneId.of("Europe/Oslo");

    private static final Comparator<PackingTrip> SOONEST_FIRST = Comparator
            .comparing(PackingTrip::getTripDate, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(PackingTrip::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()));

    private final PackingListRepository packingListRepository;
    private final PackingTripRepository packingTripRepository;
    private final LocationRepository locationRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final CategoryTemplateService categoryTemplateService;

    public PackingListService(
            PackingListRepository packingListRepository,
            PackingTripRepository packingTripRepository,
            LocationRepository locationRepository,
            InventoryItemRepository inventoryItemRepository,
            CategoryTemplateService categoryTemplateService
    ) {
        this.packingListRepository = packingListRepository;
        this.packingTripRepository = packingTripRepository;
        this.locationRepository = locationRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.categoryTemplateService = categoryTemplateService;
    }

    /**
     * Lists the trips for a location+season. If none are upcoming (e.g. the
     * last trip's date just passed), a fresh one is created from the default
     * list so there's always a list to work on.
     */
    public PackingTripsResponse listTrips(String userId, String locationId, Season season) {
        Location location = requireOwnedLocation(userId, locationId);
        LocalDate today = LocalDate.now(ZONE);
        List<PackingTrip> trips = packingTripRepository.findByLocationAndSeason(userId, location.getId(), season);

        List<PackingTrip> active = new ArrayList<>(trips.stream().filter(t -> !isPast(t, today)).toList());
        List<PackingTrip> past = new ArrayList<>(trips.stream().filter(t -> isPast(t, today)).toList());

        if (active.isEmpty()) {
            active.add(trips.isEmpty() ? createFirstTrip(userId, location, season) : newTrip(userId, location, season, null));
        }
        active.sort(SOONEST_FIRST);
        past.sort(SOONEST_FIRST.reversed());

        return new PackingTripsResponse(
                active.stream().map(t -> toResponse(t, today)).toList(),
                past.stream().map(t -> toResponse(t, today)).toList()
        );
    }

    public PackingTripResponse createTrip(String userId, String locationId, Season season, String tripDate) {
        Location location = requireOwnedLocation(userId, locationId);
        return toResponse(newTrip(userId, location, season, tripDate), LocalDate.now(ZONE));
    }

    /** Saves the trip and folds any new categories/items into the default list. */
    public PackingTripResponse saveTrip(String userId, String tripId, SavePackingTripRequest request) {
        PackingTrip trip = requireOwnedTrip(userId, tripId);
        trip.setTripDate(request.tripDate());
        trip.setNote(request.note() == null || request.note().isBlank() ? null : request.note().trim());
        trip.setCategories(request.categories().stream()
                .map(PackingListService::fromDto)
                .toList());
        trip.setUpdatedAt(null);
        packingTripRepository.save(trip);
        mergeIntoDefault(userId, trip.getLocationId(), trip.getSeason(), trip.getCategories());
        return toResponse(trip, LocalDate.now(ZONE));
    }

    /** Re-generates the trip from templates + default list + inventory, discarding edits but keeping the date. */
    public PackingTripResponse resetTrip(String userId, String tripId) {
        PackingTrip trip = requireOwnedTrip(userId, tripId);
        Location location = requireOwnedLocation(userId, trip.getLocationId());
        trip.setCategories(generateCategories(userId, location, trip.getSeason()));
        trip.setUpdatedAt(null);
        packingTripRepository.save(trip);
        return toResponse(trip, LocalDate.now(ZONE));
    }

    public void deleteTrip(String userId, String tripId) {
        requireOwnedTrip(userId, tripId);
        packingTripRepository.deleteById(tripId);
    }

    private PackingTrip newTrip(String userId, Location location, Season season, String tripDate) {
        return saveNewTrip(userId, location, season, tripDate, generateCategories(userId, location, season));
    }

    /**
     * Before trips existed, the single per-season list (now the default list)
     * held the user's in-progress checklist. Carry it over as-is, checked
     * state included, the first time trips are listed for that season.
     */
    private PackingTrip createFirstTrip(String userId, Location location, Season season) {
        PackingList legacy = packingListRepository.findById(PackingListRepository.idFor(userId, location.getId(), season));
        if (legacy == null || legacy.getCategories().isEmpty()) {
            return newTrip(userId, location, season, null);
        }
        return saveNewTrip(userId, location, season, null, legacy.getCategories());
    }

    private PackingTrip saveNewTrip(String userId, Location location, Season season, String tripDate, List<PackingCategory> categories) {
        PackingTrip trip = new PackingTrip();
        trip.setUserId(userId);
        trip.setLocationId(location.getId());
        trip.setSeason(season);
        trip.setTripDate(tripDate);
        trip.setCategories(categories);
        return packingTripRepository.save(trip);
    }

    /** Templates for the location type, plus everything ever added to this location+season's default list. */
    private List<PackingCategory> generateCategories(String userId, Location location, Season season) {
        Map<String, List<String>> names = new LinkedHashMap<>();
        for (var template : categoryTemplateService.listForLocationType(userId, location.getType())) {
            names.put(template.category(), new ArrayList<>(template.items()));
        }
        PackingList defaults = packingListRepository.findById(PackingListRepository.idFor(userId, location.getId(), season));
        if (defaults != null) {
            for (PackingCategory category : defaults.getCategories()) {
                List<String> items = names.computeIfAbsent(category.getCategory(), k -> new ArrayList<>());
                for (PackingItem item : category.getItems()) {
                    if (!containsName(items, item.getName())) {
                        items.add(item.getName());
                    }
                }
            }
        }

        List<InventoryItem> inventory = inventoryItemRepository.findByLocation(userId, location.getId());
        List<PackingCategory> categories = new ArrayList<>();
        names.forEach((category, itemNames) -> {
            List<PackingItem> items = new ArrayList<>();
            for (String name : itemNames) {
                InventoryItem match = findMatch(name, inventory);
                items.add(new PackingItem(name, match != null, match != null ? match.getId() : null));
            }
            categories.add(new PackingCategory(category, items));
        });
        return categories;
    }

    /** Adds categories/items the default list doesn't have yet. Never removes anything. */
    private void mergeIntoDefault(String userId, String locationId, Season season, List<PackingCategory> categories) {
        String id = PackingListRepository.idFor(userId, locationId, season);
        PackingList defaults = packingListRepository.findById(id);
        if (defaults == null) {
            defaults = new PackingList();
            defaults.setId(id);
            defaults.setUserId(userId);
            defaults.setLocationId(locationId);
            defaults.setSeason(season);
        }

        List<PackingCategory> merged = new ArrayList<>();
        for (PackingCategory category : defaults.getCategories()) {
            merged.add(new PackingCategory(category.getCategory(), new ArrayList<>(category.getItems())));
        }
        boolean changed = false;
        for (PackingCategory category : categories) {
            PackingCategory target = merged.stream()
                    .filter(c -> c.getCategory().equals(category.getCategory()))
                    .findFirst()
                    .orElse(null);
            if (target == null) {
                target = new PackingCategory(category.getCategory(), new ArrayList<>());
                merged.add(target);
                changed = true;
            }
            for (PackingItem item : category.getItems()) {
                List<String> existing = target.getItems().stream().map(PackingItem::getName).toList();
                if (!containsName(existing, item.getName())) {
                    target.getItems().add(new PackingItem(item.getName(), false, null));
                    changed = true;
                }
            }
        }
        if (!changed) {
            return;
        }
        defaults.setCategories(merged);
        defaults.setUpdatedAt(null);
        packingListRepository.save(defaults);
    }

    private static boolean containsName(List<String> names, String name) {
        return names.stream().anyMatch(n -> n.trim().equalsIgnoreCase(name.trim()));
    }

    private static boolean isPast(PackingTrip trip, LocalDate today) {
        return trip.getTripDate() != null && LocalDate.parse(trip.getTripDate()).isBefore(today);
    }

    private static InventoryItem findMatch(String templateItemName, List<InventoryItem> inventory) {
        for (InventoryItem item : inventory) {
            if (ItemNameMatcher.matches(templateItemName, item.getName())) {
                return item;
            }
        }
        return null;
    }

    private Location requireOwnedLocation(String userId, String locationId) {
        Location location = locationRepository.findById(locationId);
        if (location == null) {
            throw new ResponseStatusException(NOT_FOUND, "Location not found");
        }
        if (!location.getUserId().equals(userId)) {
            throw new ResponseStatusException(FORBIDDEN, "Not your location");
        }
        return location;
    }

    private PackingTrip requireOwnedTrip(String userId, String tripId) {
        PackingTrip trip = packingTripRepository.findById(tripId);
        if (trip == null) {
            throw new ResponseStatusException(NOT_FOUND, "Packing list not found");
        }
        if (!trip.getUserId().equals(userId)) {
            throw new ResponseStatusException(FORBIDDEN, "Not your packing list");
        }
        return trip;
    }

    private static PackingCategory fromDto(PackingCategoryDto dto) {
        List<PackingItem> items = dto.items().stream()
                .map(i -> new PackingItem(i.name(), i.checked(), null))
                .toList();
        return new PackingCategory(dto.category(), items);
    }

    private static PackingTripResponse toResponse(PackingTrip trip, LocalDate today) {
        List<PackingCategoryDto> categories = trip.getCategories().stream()
                .map(c -> new PackingCategoryDto(
                        c.getCategory(),
                        c.getItems().stream()
                                .map(i -> new PackingItemDto(i.getName(), i.isChecked()))
                                .toList()
                ))
                .toList();
        return new PackingTripResponse(
                trip.getId(),
                trip.getLocationId(),
                trip.getSeason().name(),
                trip.getTripDate(),
                trip.getNote(),
                isPast(trip, today),
                categories
        );
    }
}
