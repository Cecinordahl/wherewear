package com.wherewear.backend.repository;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.wherewear.backend.model.CategoryTemplate;
import com.wherewear.backend.model.LocationType;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Repository
public class CategoryTemplateRepository {

    private static final String COLLECTION = "categoryTemplates";

    private final Firestore firestore;

    public CategoryTemplateRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    public static String idFor(String userId, LocationType locationType, String category) {
        return userId + "_" + locationType + "_" + category;
    }

    public List<CategoryTemplate> findByLocationType(String userId, LocationType locationType) {
        try {
            List<QueryDocumentSnapshot> docs = firestore.collection(COLLECTION)
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("locationType", locationType.name())
                    .get().get().getDocuments();
            return docs.stream().map(d -> d.toObject(CategoryTemplate.class)).toList();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Failed to list templates for " + userId + "/" + locationType, e);
        }
    }

    public CategoryTemplate findById(String id) {
        try {
            DocumentSnapshot snapshot = firestore.collection(COLLECTION).document(id).get().get();
            return snapshot.exists() ? snapshot.toObject(CategoryTemplate.class) : null;
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Failed to read template " + id, e);
        }
    }

    public void save(CategoryTemplate template) {
        try {
            firestore.collection(COLLECTION).document(template.getId()).set(template).get();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Failed to save template " + template.getId(), e);
        }
    }
}
