package com.wherewear.backend.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.ServerTimestamp;

import java.util.Date;

/**
 * A category the user has added themselves, beyond the fixed starter set in
 * CategoryCatalog - lets the category list grow over time instead of being
 * permanently limited to what shipped with the app.
 */
public class CustomCategory {

    @DocumentId
    private String id;

    private String userId;
    private LocationType locationType;
    private String name;

    @ServerTimestamp
    private Date createdAt;

    public CustomCategory() {
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

    public LocationType getLocationType() {
        return locationType;
    }

    public void setLocationType(LocationType locationType) {
        this.locationType = locationType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
