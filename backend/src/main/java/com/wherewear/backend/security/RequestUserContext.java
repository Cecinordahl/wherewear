package com.wherewear.backend.security;

/**
 * Holds the authenticated Firebase uid for the current request thread.
 * Set by FirebaseAuthFilter, read by controllers/services, cleared at the
 * end of the request.
 */
public final class RequestUserContext {

    private static final ThreadLocal<String> CURRENT_USER_ID = new ThreadLocal<>();

    private RequestUserContext() {
    }

    static void set(String userId) {
        CURRENT_USER_ID.set(userId);
    }

    static void clear() {
        CURRENT_USER_ID.remove();
    }

    public static String requireUserId() {
        String userId = CURRENT_USER_ID.get();
        if (userId == null) {
            throw new IllegalStateException("No authenticated user on this request thread");
        }
        return userId;
    }
}
