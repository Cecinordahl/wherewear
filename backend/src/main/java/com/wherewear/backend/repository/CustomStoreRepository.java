package com.wherewear.backend.repository;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.wherewear.backend.model.CustomStore;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Repository
public class CustomStoreRepository {

    private static final String COLLECTION = "customStores";

    private final Firestore firestore;

    public CustomStoreRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    public List<CustomStore> findAllForUser(String userId) {
        try {
            List<QueryDocumentSnapshot> docs = firestore.collection(COLLECTION)
                    .whereEqualTo("userId", userId)
                    .get().get().getDocuments();
            return docs.stream().map(d -> d.toObject(CustomStore.class)).toList();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Failed to list custom stores for user " + userId, e);
        }
    }

    public CustomStore findById(String id) {
        try {
            DocumentSnapshot snapshot = firestore.collection(COLLECTION).document(id).get().get();
            return snapshot.exists() ? snapshot.toObject(CustomStore.class) : null;
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Failed to read custom store " + id, e);
        }
    }

    public CustomStore save(CustomStore store) {
        try {
            if (store.getId() == null) {
                var docRef = firestore.collection(COLLECTION).document();
                store.setId(docRef.getId());
                docRef.set(store).get();
            } else {
                firestore.collection(COLLECTION).document(store.getId()).set(store).get();
            }
            return store;
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Failed to save custom store", e);
        }
    }

    public void deleteById(String id) {
        try {
            firestore.collection(COLLECTION).document(id).delete().get();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Failed to delete custom store " + id, e);
        }
    }
}
