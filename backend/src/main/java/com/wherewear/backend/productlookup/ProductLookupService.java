package com.wherewear.backend.productlookup;

import com.wherewear.backend.dto.ProductLookupDtos.ProductCandidate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;

@Service
public class ProductLookupService {

    private final TempFileHostClient tempFileHostClient;
    private final SerpApiClient serpApiClient;
    private final ImageCompressor imageCompressor;

    public ProductLookupService(TempFileHostClient tempFileHostClient, SerpApiClient serpApiClient, ImageCompressor imageCompressor) {
        this.tempFileHostClient = tempFileHostClient;
        this.serpApiClient = serpApiClient;
        this.imageCompressor = imageCompressor;
    }

    /**
     * When a store URL is known, this prioritizes a site-scoped regular web
     * search over Google Shopping - Shopping only covers retailers in
     * Google's Shopping Graph, which smaller/regional stores often aren't
     * in, so it can come back empty even when the store's own site has the
     * product. Falls back to a plain shopping search (item + store name)
     * otherwise, or when the site-scoped search finds nothing.
     */
    public List<ProductCandidate> searchByText(String query, String storeName, String storeUrl) {
        String domain = extractDomain(storeUrl);
        if (domain != null) {
            List<ProductCandidate> siteResults = serpApiClient.searchOnSite(query, domain);
            if (!siteResults.isEmpty()) {
                return siteResults;
            }
        }
        String combinedQuery = (storeName != null && !storeName.isBlank()) ? query + " " + storeName : query;
        return serpApiClient.searchByText(combinedQuery);
    }

    private static String extractDomain(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        try {
            String host = new URI(url).getHost();
            if (host == null) {
                return null;
            }
            return host.startsWith("www.") ? host.substring(4) : host;
        } catch (URISyntaxException e) {
            return null;
        }
    }

    public List<ProductCandidate> searchByPhoto(MultipartFile photo) {
        String contentType = photo.getContentType() != null ? photo.getContentType() : "image/jpeg";
        String tempUrl;
        try {
            tempUrl = tempFileHostClient.upload(photo.getBytes(), contentType);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new ResponseStatusException(BAD_GATEWAY, "Failed to upload photo for search", e);
        }
        return serpApiClient.searchByImageUrl(tempUrl);
    }

    /** Fetches the chosen candidate's image and returns it compressed as a data URL, ready to store. */
    public String resolvePhotoDataUrl(String sourceImageUrl) {
        return imageCompressor.fetchAndCompressAsDataUrl(sourceImageUrl);
    }
}
