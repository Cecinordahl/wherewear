package com.wherewear.backend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.cloud.FirestoreClient;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Initializes the Firebase Admin SDK from either a service account file on
 * disk (local dev: wherewear.firebase.service-account-path) or the raw JSON
 * in an env var (Render deploy: wherewear.firebase.service-account-json,
 * since Render's free tier has no persistent disk to upload a file to).
 * See the README "Firebase project setup" section for how to obtain this key.
 */
@Configuration
public class FirebaseConfig {

    @Value("${wherewear.firebase.service-account-path:}")
    private String serviceAccountPath;

    @Value("${wherewear.firebase.service-account-json:}")
    private String serviceAccountJson;

    @PostConstruct
    public void init() throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            return;
        }

        GoogleCredentials credentials = loadCredentials();

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .build();

        FirebaseApp.initializeApp(options);
    }

    private GoogleCredentials loadCredentials() throws IOException {
        if (!serviceAccountJson.isBlank()) {
            try (InputStream in = new ByteArrayInputStream(serviceAccountJson.getBytes(StandardCharsets.UTF_8))) {
                return GoogleCredentials.fromStream(in);
            }
        }
        if (!serviceAccountPath.isBlank()) {
            try (InputStream in = new FileInputStream(serviceAccountPath)) {
                return GoogleCredentials.fromStream(in);
            }
        }
        throw new IllegalStateException(
                "No Firebase credentials configured. Set FIREBASE_SERVICE_ACCOUNT_PATH (local) " +
                        "or FIREBASE_SERVICE_ACCOUNT_JSON (deploy). See README.md."
        );
    }

    @Bean
    public Firestore firestore() {
        return FirestoreClient.getFirestore();
    }

    @Bean
    public FirebaseAuth firebaseAuth() {
        return FirebaseAuth.getInstance();
    }
}
