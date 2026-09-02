package com.wherewear.backend.repository;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.wherewear.backend.model.AppUser;
import org.springframework.stereotype.Repository;

import java.util.concurrent.ExecutionException;

@Repository
public class AppUserRepository {

    private static final String COLLECTION = "users";

    private final Firestore firestore;

    public AppUserRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    public AppUser findById(String userId) {
        try {
            DocumentSnapshot snapshot = firestore.collection(COLLECTION).document(userId).get().get();
            return snapshot.exists() ? snapshot.toObject(AppUser.class) : null;
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Failed to read user " + userId, e);
        }
    }

    public void create(AppUser user) {
        try {
            firestore.collection(COLLECTION).document(user.getId()).set(user).get();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Failed to create user " + user.getId(), e);
        }
    }
}
