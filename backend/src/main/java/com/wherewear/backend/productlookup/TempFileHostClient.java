package com.wherewear.backend.productlookup;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Uploads a photo to a free, anonymous, no-signup temp-file host (0x0.st) so
 * SerpAPI's Google Lens engine has a public URL to fetch - it doesn't accept
 * raw image uploads. The file is only needed for the few seconds it takes
 * SerpAPI to fetch it; 0x0.st expires anonymous uploads after a short time
 * on its own. See README "Product photo lookup" for the trade-offs of this
 * approach (a small third-party service, chosen to avoid requiring the
 * Firebase Blaze plan just for this one feature).
 */
@Component
class TempFileHostClient {

    private static final String UPLOAD_URL = "https://0x0.st";
    private static final String BOUNDARY = "WherewearBoundary" + UUID.randomUUID();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    String upload(byte[] imageBytes, String contentType) throws IOException, InterruptedException {
        byte[] body = buildMultipartBody(imageBytes, contentType);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(UPLOAD_URL))
                .header("Content-Type", "multipart/form-data; boundary=" + BOUNDARY)
                .header("User-Agent", "wherewear-app")
                .timeout(Duration.ofSeconds(20))
                .POST(BodyPublishers.ofByteArray(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Temp file host upload failed with status " + response.statusCode());
        }
        return response.body().trim();
    }

    private static byte[] buildMultipartBody(byte[] imageBytes, String contentType) throws IOException {
        String header = "--" + BOUNDARY + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"photo.jpg\"\r\n"
                + "Content-Type: " + contentType + "\r\n\r\n";
        String footer = "\r\n--" + BOUNDARY + "--\r\n";

        List<byte[]> parts = new ArrayList<>();
        parts.add(header.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        parts.add(imageBytes);
        parts.add(footer.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        int totalLength = parts.stream().mapToInt(p -> p.length).sum();
        byte[] result = new byte[totalLength];
        int offset = 0;
        for (byte[] part : parts) {
            System.arraycopy(part, 0, result, offset, part.length);
            offset += part.length;
        }
        return result;
    }
}
