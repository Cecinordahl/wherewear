package com.wherewear.backend.repository;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.wherewear.backend.model.InventoryItem;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Repository
public class InventoryItemRepository {

    private static final String COLLECTION = "inventoryItems";

    private final Firestore firestore;

    public InventoryItemRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    public List<InventoryItem> findByLocation(String userId, String locationId) {
        try {
            List<QueryDocumentSnapshot> docs = firestore.collection(COLLECTION)
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("locationId", locationId)
                    .get().get().getDocuments();
            return docs.stream().map(d -> d.toObject(InventoryItem.class)).toList();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Failed to list items for location " + locationId, e);
        }
    }

    public List<InventoryItem> findAllForUser(String userId) {
        try {
            List<QueryDocumentSnapshot> docs = firestore.collection(COLLECTION)
                    .whereEqualTo("userId", userId)
                    .get().get().getDocuments();
            return docs.stream().map(d -> d.toObject(InventoryItem.class)).toList();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Failed to list items for user " + userId, e);
        }
    }

    public InventoryItem findById(String id) {
        try {
            DocumentSnapshot snapshot = firestore.collection(COLLECTION).document(id).get().get();
            return snapshot.exists() ? snapshot.toObject(InventoryItem.class) : null;
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Failed to read item " + id, e);
        }
    }

    public InventoryItem save(InventoryItem item) {
        try {
            if (item.getId() == null) {
                var docRef = firestore.collection(COLLECTION).document();
                item.setId(docRef.getId());
                docRef.set(item).get();
            } else {
                firestore.collection(COLLECTION).document(item.getId()).set(item).get();
            }
            return item;
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Failed to save item", e);
        }
    }

    public void deleteById(String id) {
        try {
            firestore.collection(COLLECTION).document(id).delete().get();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Failed to delete item " + id, e);
        }
    }
}
