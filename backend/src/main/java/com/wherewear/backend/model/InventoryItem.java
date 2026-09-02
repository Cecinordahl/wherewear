package com.wherewear.backend.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.ServerTimestamp;

import java.util.Date;

public class InventoryItem {

    @DocumentId
    private String id;

    private String userId;
    private String locationId;
    private String category;
    private String name;

    // A compressed "data:image/jpeg;base64,..." string, populated when the
    // user picks a matched product photo via the product-lookup feature.
    // Stored inline on the doc (not Firebase Storage) to stay on the free
    // Firestore Spark plan - see README "Product photo lookup" section.
    private String photoDataUrl;

    @ServerTimestamp
    private Date createdAt;

    @ServerTimestamp
    private Date updatedAt;

    public InventoryItem() {
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhotoDataUrl() {
        return photoDataUrl;
    }

    public void setPhotoDataUrl(String photoDataUrl) {
        this.photoDataUrl = photoDataUrl;
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
