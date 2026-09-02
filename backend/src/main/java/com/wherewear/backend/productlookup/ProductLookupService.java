package com.wherewear.backend.productlookup;

import com.wherewear.backend.dto.ProductLookupDtos.ProductCandidate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
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

    public List<ProductCandidate> searchByText(String query) {
        return serpApiClient.searchByText(query);
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
