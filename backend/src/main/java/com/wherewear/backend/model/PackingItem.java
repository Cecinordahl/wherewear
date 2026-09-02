package com.wherewear.backend.model;

public class PackingItem {

    private String name;
    private boolean checked;

    // Set when this item was auto-matched against an existing inventory item
    // at generation time; null for items added manually while editing.
    private String sourceInventoryItemId;

    public PackingItem() {
    }

    public PackingItem(String name, boolean checked, String sourceInventoryItemId) {
        this.name = name;
        this.checked = checked;
        this.sourceInventoryItemId = sourceInventoryItemId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    public String getSourceInventoryItemId() {
        return sourceInventoryItemId;
    }

    public void setSourceInventoryItemId(String sourceInventoryItemId) {
        this.sourceInventoryItemId = sourceInventoryItemId;
    }
}
