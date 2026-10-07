package com.wherewear.backend.repository;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.wherewear.backend.model.PackingTrip;
import com.wherewear.backend.model.Season;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Repository
public class PackingTripRepository {

    private static final String COLLECTION = "packingTrips";

    private final Firestore firestore;

    public PackingTripRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    public List<PackingTrip> findByLocationAndSeason(String userId, String locationId, Season season) {
        try {
            List<QueryDocumentSnapshot> docs = firestore.collection(COLLECTION)
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("locationId", locationId)
                    .whereEqualTo("season", season.name())
                    .get().get().getDocuments();
            return docs.stream().map(d -> d.toObject(PackingTrip.class)).toList();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Failed to list packing trips for " + locationId + "/" + season, e);
        }
    }

    public PackingTrip findById(String id) {
        try {
            DocumentSnapshot snapshot = firestore.collection(COLLECTION).document(id).get().get();
            return snapshot.exists() ? snapshot.toObject(PackingTrip.class) : null;
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Failed to read packing trip " + id, e);
        }
    }

    public PackingTrip save(PackingTrip trip) {
        try {
            if (trip.getId() == null) {
                var docRef = firestore.collection(COLLECTION).document();
                trip.setId(docRef.getId());
                docRef.set(trip).get();
            } else {
                firestore.collection(COLLECTION).document(trip.getId()).set(trip).get();
            }
            return trip;
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Failed to save packing trip", e);
        }
    }

    public void deleteById(String id) {
        try {
            firestore.collection(COLLECTION).document(id).delete().get();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Failed to delete packing trip " + id, e);
        }
    }
}
