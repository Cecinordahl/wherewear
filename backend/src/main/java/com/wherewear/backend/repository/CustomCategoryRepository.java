package com.wherewear.backend.repository;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.wherewear.backend.model.CustomCategory;
import com.wherewear.backend.model.LocationType;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Repository
public class CustomCategoryRepository {

    private static final String COLLECTION = "customCategories";

    private final Firestore firestore;

    public CustomCategoryRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    public List<CustomCategory> findByLocationType(String userId, LocationType locationType) {
        try {
            List<QueryDocumentSnapshot> docs = firestore.collection(COLLECTION)
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("locationType", locationType.name())
                    .get().get().getDocuments();
            return docs.stream().map(d -> d.toObject(CustomCategory.class)).toList();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Failed to list custom categories for " + userId + "/" + locationType, e);
        }
    }

    public CustomCategory save(CustomCategory category) {
        try {
            var docRef = firestore.collection(COLLECTION).document();
            category.setId(docRef.getId());
            docRef.set(category).get();
            return category;
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Failed to save custom category", e);
        }
    }
}
