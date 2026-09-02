package com.wherewear.backend.repository;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.wherewear.backend.model.Location;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Repository
public class LocationRepository {

    private static final String COLLECTION = "locations";

    private final Firestore firestore;

    public LocationRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    public List<Location> findAllForUser(String userId) {
        try {
            List<QueryDocumentSnapshot> docs = firestore.collection(COLLECTION)
                    .whereEqualTo("userId", userId)
                    .get().get().getDocuments();
            return docs.stream().map(d -> d.toObject(Location.class)).toList();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Failed to list locations for user " + userId, e);
        }
    }

    public Location findById(String id) {
        try {
            DocumentSnapshot snapshot = firestore.collection(COLLECTION).document(id).get().get();
            return snapshot.exists() ? snapshot.toObject(Location.class) : null;
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Failed to read location " + id, e);
        }
    }

    public Location save(Location location) {
        try {
            if (location.getId() == null) {
                var docRef = firestore.collection(COLLECTION).document();
                location.setId(docRef.getId());
                docRef.set(location).get();
            } else {
                firestore.collection(COLLECTION).document(location.getId()).set(location).get();
            }
            return location;
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Failed to save location", e);
        }
    }

    public void deleteById(String id) {
        try {
            firestore.collection(COLLECTION).document(id).delete().get();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Failed to delete location " + id, e);
        }
    }
}
