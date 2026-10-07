package com.wherewear.backend.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.ServerTimestamp;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * One concrete packing list for a trip to a location in a given season.
 * Several can exist per location+season (planning ahead); once tripDate has
 * passed the trip is treated as historic. Created from the location+season's
 * default list (see PackingList).
 */
public class PackingTrip {

    @DocumentId
    private String id;

    private String userId;
    private String locationId;
    private Season season;

    // ISO yyyy-MM-dd, stored as a plain string like ShoppingListItem.tripDate.
    // Null means "no date yet" - such trips never become historic.
    private String tripDate;

    private List<PackingCategory> categories = new ArrayList<>();

    @ServerTimestamp
    private Date createdAt;

    @ServerTimestamp
    private Date updatedAt;

    public PackingTrip() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    public Season getSeason() {
        return season;
    }

    public void setSeason(Season season) {
        this.season = season;
    }

    public String getTripDate() {
        return tripDate;
    }

    public void setTripDate(String tripDate) {
        this.tripDate = tripDate;
    }

    public List<PackingCategory> getCategories() {
        return categories;
    }

    public void setCategories(List<PackingCategory> categories) {
        this.categories = categories;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
}
