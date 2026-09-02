package com.wherewear.backend.productlookup;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wherewear.backend.dto.ProductLookupDtos.ProductCandidate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

/**
 * Thin client for the SerpAPI engines this app uses:
 * - google_lens: reverse image search from a public image URL
 * - google_shopping: text -> product search (has images, but coverage is
 *   patchy for smaller/regional retailers not in Google's Shopping Graph)
 * - google (organic): text + "site:<domain>" -> a much more reliable way to
 *   find a specific product page on a specific store's site, since regular
 *   web search indexes far more broadly than Shopping - used when the store
 *   picked in the shopping list has a known URL (see ProductLookupService).
 * Region params are fixed to Norway since that's this app's context; free
 * tier is 100 searches/month total across all of the above. See README.
 */
@Component
class SerpApiClient {

    private static final String BASE_URL = "https://serpapi.com/search.json";
    private static final String REGION_PARAMS = "&gl=no&hl=no&google_domain=google.no";
    private static final int MAX_CANDIDATES = 12;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${wherewear.serpapi.api-key}")
    private String apiKey;

    List<ProductCandidate> searchByImageUrl(String imageUrl) {
        requireApiKey();
        String url = BASE_URL + "?engine=google_lens"
                + "&url=" + encode(imageUrl)
                + REGION_PARAMS
                + "&api_key=" + encode(apiKey);
        JsonNode root = fetch(url);
        return extractCandidates(root, "visual_matches", true);
    }

    List<ProductCandidate> searchByText(String query) {
        requireApiKey();
        String url = BASE_URL + "?engine=google_shopping"
                + "&q=" + encode(query)
                + REGION_PARAMS
                + "&api_key=" + encode(apiKey);
        JsonNode root = fetch(url);
        return extractCandidates(root, "shopping_results", true);
    }

    /** Regular web search scoped to one site - more reliable than Shopping for a specific known retailer. */
    List<ProductCandidate> searchOnSite(String query, String siteDomain) {
        requireApiKey();
        String url = BASE_URL + "?engine=google"
                + "&q=" + encode(query + " site:" + siteDomain)
                + REGION_PARAMS
                + "&api_key=" + encode(apiKey);
        JsonNode root = fetch(url);
        return extractCandidates(root, "organic_results", false);
    }

    private void requireApiKey() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ResponseStatusException(SERVICE_UNAVAILABLE,
                    "Product photo lookup isn't configured yet (missing SERPAPI_API_KEY). See README.");
        }
    }

    private JsonNode fetch(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(20))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new ResponseStatusException(BAD_GATEWAY, "SerpAPI request failed: " + response.statusCode());
            }
            return objectMapper.readTree(response.body());
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new ResponseStatusException(BAD_GATEWAY, "Failed to reach SerpAPI", e);
        }
    }

    /**
     * Defensively pulls out the handful of fields we need - SerpAPI's exact
     * response shape can vary slightly between engines/over time, so this
     * skips any entry missing what we need rather than failing the request.
     * requireImage is false for organic web results, which don't carry one.
     */
    private static List<ProductCandidate> extractCandidates(JsonNode root, String arrayField, boolean requireImage) {
        List<ProductCandidate> candidates = new ArrayList<>();
        JsonNode array = root.path(arrayField);
        if (!array.isArray()) {
            return candidates;
        }
        for (JsonNode node : array) {
            String imageUrl = firstNonBlank(node, "thumbnail", "image", "original");
            if (requireImage && imageUrl == null) {
                continue;
            }
            String title = firstNonBlank(node, "title");
            String source = firstNonBlank(node, "source", "displayed_link");
            String pageUrl = firstNonBlank(node, "link", "source_url");
            candidates.add(new ProductCandidate(
                    title != null ? title : "Ukjent produkt",
                    source,
                    pageUrl,
                    imageUrl
            ));
            if (candidates.size() >= MAX_CANDIDATES) {
                break;
            }
        }
        return candidates;
    }

    private static String firstNonBlank(JsonNode node, String... fields) {
        for (String field : fields) {
            String value = node.path(field).asText(null);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
