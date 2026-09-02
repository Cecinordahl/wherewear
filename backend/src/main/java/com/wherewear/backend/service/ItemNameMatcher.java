package com.wherewear.backend.service;

/**
 * Simple case-insensitive substring matcher used to auto-check packing-list
 * items against existing inventory on first generation. Not meant to be
 * perfect - the user corrects mismatches by hand (see project spec).
 */
final class ItemNameMatcher {

    private ItemNameMatcher() {
    }

    static boolean matches(String templateItemName, String inventoryItemName) {
        String a = normalize(templateItemName);
        String b = normalize(inventoryItemName);
        if (a.isEmpty() || b.isEmpty()) {
            return false;
        }
        return a.contains(b) || b.contains(a);
    }

    static String normalize(String value) {
        // Strip a trailing quantity marker like "x5" before comparing, so
        // "Truser x5" (template) still matches "truser" (inventory).
        return value.toLowerCase().replaceAll("\\s*x\\d+\\s*$", "").trim();
    }
}
