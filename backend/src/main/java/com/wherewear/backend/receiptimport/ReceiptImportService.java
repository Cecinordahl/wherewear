package com.wherewear.backend.receiptimport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wherewear.backend.catalog.CategoryCatalog;
import com.wherewear.backend.dto.ReceiptImportDtos.ReceiptItemCandidate;
import com.wherewear.backend.model.LocationType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
public class ReceiptImportService {

    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ReceiptImportService(GeminiClient geminiClient) {
        this.geminiClient = geminiClient;
    }

    public List<ReceiptItemCandidate> extractItems(MultipartFile photo, LocationType locationType) {
        List<String> categories = CategoryCatalog.categoriesFor(locationType);

        byte[] bytes;
        try {
            bytes = photo.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(BAD_REQUEST, "Couldn't read the uploaded photo", e);
        }
        String contentType = photo.getContentType() != null ? photo.getContentType() : "image/jpeg";

        String rawJson = geminiClient.generateJson(buildPrompt(categories), bytes, contentType);
        return parseCandidates(rawJson, categories);
    }

    private static String buildPrompt(List<String> categories) {
        return """
                You are reading a shopping receipt (photo attached), which may be in any language.
                List every purchased clothing/gear line item (skip subtotals, tax lines, payment/card
                details, store info, and standalone ref/RFID code lines).
                For each item, write a short, clean, human-readable product name (translate and clean
                up cryptic abbreviations if needed - e.g. "TS COMFORT LITE W MULTI WHITE MAGNOLIA 38"
                could become "Hvit magnolia t-skjorte, str 38"), pick the single best-fitting
                category from exactly this list: %s, and if a brand name is identifiable (from the
                store name, a logo, or the item description) include it - otherwise omit it or use null.
                Respond with ONLY a JSON array, no other text, in this shape:
                [{"name": "...", "category": "...", "brand": "..." or null}]
                """.formatted(String.join(", ", categories));
    }

    private List<ReceiptItemCandidate> parseCandidates(String rawJson, List<String> validCategories) {
        JsonNode array;
        try {
            array = objectMapper.readTree(rawJson);
        } catch (IOException e) {
            throw new ResponseStatusException(BAD_REQUEST, "Couldn't read the receipt - try a clearer photo.", e);
        }
        if (!array.isArray()) {
            return List.of();
        }

        List<ReceiptItemCandidate> candidates = new ArrayList<>();
        String fallbackCategory = validCategories.get(0);
        for (JsonNode node : array) {
            String name = node.path("name").asText(null);
            if (name == null || name.isBlank()) {
                continue;
            }
            String category = node.path("category").asText(null);
            if (category == null || !validCategories.contains(category)) {
                category = fallbackCategory;
            }
            String brand = node.path("brand").asText(null);
            candidates.add(new ReceiptItemCandidate(name.trim(), category, brand != null && !brand.isBlank() ? brand.trim() : null));
        }
        return candidates;
    }
}
