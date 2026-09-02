package com.wherewear.backend.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.ServerTimestamp;

import java.util.Date;

/**
 * A store the user has added themselves, beyond the small hardcoded starter
 * list in the frontend (stores.ts) - lets the "which store" autocomplete
 * grow over time instead of only ever offering the same handful of names.
 */
public class CustomStore {

    @DocumentId
    private String id;

    private String userId;
    private String name;

    // Optional - if known, enables a much more reliable site-scoped product
    // search (see ProductLookupService) instead of a generic text search.
    private String url;

    @ServerTimestamp
    private Date createdAt;

    public CustomStore() {
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
