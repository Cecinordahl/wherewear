package com.wherewear.backend.receiptimport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wherewear.backend.catalog.CategoryCatalog;
import com.wherewear.backend.dto.PhotoImportDtos.PhotoItemCandidate;
import com.wherewear.backend.model.LocationType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

/**
 * Identifies the single clothing/gear item in a photo (via Gemini) - the
 * photo counterpart to ReceiptImportService. Lives in this package to share
 * the package-private GeminiClient.
 */
@Service
public class PhotoImportService {

    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PhotoImportService(GeminiClient geminiClient) {
        this.geminiClient = geminiClient;
    }

    public PhotoItemCandidate identifyItem(MultipartFile photo, LocationType locationType) {
        List<String> categories = CategoryCatalog.categoriesFor(locationType);

        byte[] bytes;
        try {
            bytes = photo.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(BAD_REQUEST, "Couldn't read the uploaded photo", e);
        }
        String contentType = photo.getContentType() != null ? photo.getContentType() : "image/jpeg";

        String rawJson = geminiClient.generateJson(buildPrompt(categories), bytes, contentType);
        return parseCandidate(rawJson, categories);
    }

    private static String buildPrompt(List<String> categories) {
        return """
                The attached photo shows a single clothing item, shoe, accessory or piece of gear.
                Write a short, human-readable Norwegian name for it that includes the colour and type
                (e.g. "Marineblå dunjakke", "Hvite joggesko", "Grå ullsokker"). Pick the single
                best-fitting category from exactly this list: %s. If a brand is identifiable from a
                visible logo or label, include it - otherwise use null. Don't guess a brand.
                Respond with ONLY a JSON object, no other text, in this shape:
                {"name": "...", "category": "...", "brand": "..." or null}
                """.formatted(String.join(", ", categories));
    }

    private PhotoItemCandidate parseCandidate(String rawJson, List<String> validCategories) {
        JsonNode node;
        try {
            node = objectMapper.readTree(rawJson);
        } catch (IOException e) {
            throw new ResponseStatusException(BAD_REQUEST, "Couldn't identify the item - try a clearer photo.", e);
        }
        // The model occasionally wraps a lone object in an array despite the prompt.
        if (node.isArray() && !node.isEmpty()) {
            node = node.get(0);
        }

        String name = node.path("name").asText("");
        String category = node.path("category").asText(null);
        if (category == null || !validCategories.contains(category)) {
            category = validCategories.get(0);
        }
        String brand = node.path("brand").asText(null);
        return new PhotoItemCandidate(
                name.trim(), category, brand != null && !brand.isBlank() && !brand.equals("null") ? brand.trim() : null);
    }
}
