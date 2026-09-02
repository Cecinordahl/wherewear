package com.wherewear.backend.repository;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.wherewear.backend.model.PackingList;
import com.wherewear.backend.model.Season;
import org.springframework.stereotype.Repository;

import java.util.concurrent.ExecutionException;

@Repository
public class PackingListRepository {

    private static final String COLLECTION = "packingLists";

    private final Firestore firestore;

    public PackingListRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    public static String idFor(String userId, String locationId, Season season) {
        return userId + "_" + locationId + "_" + season;
    }

    public PackingList findById(String id) {
        try {
            DocumentSnapshot snapshot = firestore.collection(COLLECTION).document(id).get().get();
            return snapshot.exists() ? snapshot.toObject(PackingList.class) : null;
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Failed to read packing list " + id, e);
        }
    }

    public void save(PackingList packingList) {
        try {
            firestore.collection(COLLECTION).document(packingList.getId()).set(packingList).get();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Failed to save packing list " + packingList.getId(), e);
        }
    }
}
