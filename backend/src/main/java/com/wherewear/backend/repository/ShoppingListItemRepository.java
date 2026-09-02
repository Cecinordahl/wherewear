package com.wherewear.backend.repository;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.wherewear.backend.model.ShoppingListItem;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Repository
public class ShoppingListItemRepository {

    private static final String COLLECTION = "shoppingListItems";

    private final Firestore firestore;

    public ShoppingListItemRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    public List<ShoppingListItem> findAllForUser(String userId) {
        try {
            List<QueryDocumentSnapshot> docs = firestore.collection(COLLECTION)
                    .whereEqualTo("userId", userId)
                    .get().get().getDocuments();
            return docs.stream().map(d -> d.toObject(ShoppingListItem.class)).toList();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Failed to list shopping list items for user " + userId, e);
        }
    }

    public ShoppingListItem findById(String id) {
        try {
            DocumentSnapshot snapshot = firestore.collection(COLLECTION).document(id).get().get();
            return snapshot.exists() ? snapshot.toObject(ShoppingListItem.class) : null;
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Failed to read shopping list item " + id, e);
        }
    }

    public ShoppingListItem save(ShoppingListItem item) {
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
            throw new IllegalStateException("Failed to save shopping list item", e);
        }
    }

    public void deleteById(String id) {
        try {
            firestore.collection(COLLECTION).document(id).delete().get();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Failed to delete shopping list item " + id, e);
        }
    }
}
