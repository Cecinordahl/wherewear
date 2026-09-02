package com.wherewear.backend.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.ServerTimestamp;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PackingList {

    // Deterministic: {userId}_{locationId}_{season}
    @DocumentId
    private String id;

    private String userId;
    private String locationId;
    private Season season;
    private List<PackingCategory> categories = new ArrayList<>();

    @ServerTimestamp
    private Date generatedAt;

    @ServerTimestamp
    private Date updatedAt;

    public PackingList() {
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

    public List<PackingCategory> getCategories() {
        return categories;
    }

    public void setCategories(List<PackingCategory> categories) {
        this.categories = categories;
    }

    public Date getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(Date generatedAt) {
        this.generatedAt = generatedAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
}
