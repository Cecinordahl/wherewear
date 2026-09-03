package com.wherewear.backend.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.ServerTimestamp;

import java.util.Date;

public class ShoppingListItem {

    @DocumentId
    private String id;

    private String userId;
    private String name;

    // Where/how to buy it: a real Location id, one of the fixed sentinels
    // "ONLINE" or "HOME" (not real Locations, just convenience defaults), or
    // null for "anywhere". See ShoppingListDtos for the sentinel constants.
    private String locationId;

    // Which wardrobe/Location this item is actually needed for - independent
    // of locationId (where/how to buy it). E.g. buy in Norway (locationId
    // HOME) for the Spain wardrobe (neededForLocationId = that Location's
    // id). Always a real Location id or null ("no particular wardrobe"),
    // never the ONLINE/HOME purchase sentinels.
    private String neededForLocationId;

    private boolean checked;

    // Optional "order in time for a trip" reminder, only meaningful when
    // locationId is the ONLINE sentinel. Plain ISO date (yyyy-MM-dd) / a day
    // count - no time-of-day, kept as a String to avoid timezone conversion
    // bugs for something that's really just a calendar date. tripDate may be
    // null even when online, meaning "don't know the date yet".
    private String tripDate;
    private Integer leadTimeDays;

    // Optional free-text store name (e.g. "Bikbok"). storeUrl is populated
    // automatically when the name matches an entry in the frontend's known-
    // store list at the time it was picked (see frontend stores.ts) - stored
    // rather than re-resolved later so the link stays stable even if that
    // list changes. productUrl is an optional manual link to the exact item
    // (e.g. a copied product page URL), which takes priority for display.
    private String storeName;
    private String storeUrl;
    private String productUrl;

    @ServerTimestamp
    private Date createdAt;

    @ServerTimestamp
    private Date updatedAt;

    public ShoppingListItem() {
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

    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    public String getNeededForLocationId() {
        return neededForLocationId;
    }

    public void setNeededForLocationId(String neededForLocationId) {
        this.neededForLocationId = neededForLocationId;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    public String getTripDate() {
        return tripDate;
    }

    public void setTripDate(String tripDate) {
        this.tripDate = tripDate;
    }

    public Integer getLeadTimeDays() {
        return leadTimeDays;
    }

    public void setLeadTimeDays(Integer leadTimeDays) {
        this.leadTimeDays = leadTimeDays;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public String getStoreUrl() {
        return storeUrl;
    }

    public void setStoreUrl(String storeUrl) {
        this.storeUrl = storeUrl;
    }

    public String getProductUrl() {
        return productUrl;
    }

    public void setProductUrl(String productUrl) {
        this.productUrl = productUrl;
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
