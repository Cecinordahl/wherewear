package com.wherewear.backend.model;

import java.util.ArrayList;
import java.util.List;

public class PackingCategory {

    private String category;
    private List<PackingItem> items = new ArrayList<>();

    public PackingCategory() {
    }

    public PackingCategory(String category, List<PackingItem> items) {
        this.category = category;
        this.items = items;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public List<PackingItem> getItems() {
        return items;
    }

    public void setItems(List<PackingItem> items) {
        this.items = items;
    }
}
