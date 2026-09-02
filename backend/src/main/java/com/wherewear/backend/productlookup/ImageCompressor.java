package com.wherewear.backend.productlookup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.Iterator;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

/**
 * Fetches an image from a URL and re-encodes it as a small JPEG, so it fits
 * comfortably inside a Firestore document (1 MiB limit) - see the note on
 * InventoryItem.photoDataUrl for why images live inline rather than in
 * Firebase Storage.
 */
@Component
class ImageCompressor {

    private static final Logger log = LoggerFactory.getLogger(ImageCompressor.class);

    private static final int MAX_DIMENSION = 900;
    private static final float JPEG_QUALITY = 0.72f;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /** Returns a "data:image/jpeg;base64,..." string. */
    String fetchAndCompressAsDataUrl(String imageUrl) {
        byte[] rawBytes = fetch(imageUrl);
        BufferedImage image = decode(rawBytes);
        byte[] jpegBytes = toCompressedJpeg(image);
        return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(jpegBytes);
    }

    private byte[] fetch(String imageUrl) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(imageUrl))
                    .timeout(Duration.ofSeconds(20))
                    .header("User-Agent", "wherewear-app")
                    .GET()
                    .build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) {
                log.warn("Failed to fetch image from {}: status {}", imageUrl, response.statusCode());
                throw new ResponseStatusException(BAD_GATEWAY, "Failed to fetch image (status " + response.statusCode() + ")");
            }
            return response.body();
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.warn("Failed to fetch image from {}", imageUrl, e);
            throw new ResponseStatusException(BAD_GATEWAY, "Failed to fetch image", e);
        }
    }

    private static BufferedImage decode(byte[] bytes) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null) {
                log.warn("No ImageIO reader could decode {} bytes (unsupported format?)", bytes.length);
                throw new ResponseStatusException(BAD_REQUEST, "That URL isn't a readable image (unsupported format)");
            }
            return image;
        } catch (IOException e) {
            log.warn("Failed to decode image", e);
            throw new ResponseStatusException(BAD_REQUEST, "That URL isn't a readable image", e);
        }
    }

    private static byte[] toCompressedJpeg(BufferedImage original) {
        BufferedImage resized = resizeIfNeeded(original);

        // Drop alpha channel - JPEG doesn't support it, and source images
        // (e.g. PNG thumbnails) may have one.
        BufferedImage rgb = new BufferedImage(resized.getWidth(), resized.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        g.drawImage(resized, 0, 0, null);
        g.dispose();

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
            ImageWriter writer = writers.next();
            ImageWriteParam params = writer.getDefaultWriteParam();
            params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            params.setCompressionQuality(JPEG_QUALITY);

            writer.setOutput(new MemoryCacheImageOutputStream(out));
            writer.write(null, new IIOImage(rgb, null, null), params);
            writer.dispose();
            return out.toByteArray();
        } catch (IOException e) {
            throw new ResponseStatusException(BAD_GATEWAY, "Failed to compress image", e);
        }
    }

    private static BufferedImage resizeIfNeeded(BufferedImage original) {
        int width = original.getWidth();
        int height = original.getHeight();
        int longestSide = Math.max(width, height);
        if (longestSide <= MAX_DIMENSION) {
            return original;
        }

        double scale = (double) MAX_DIMENSION / longestSide;
        int newWidth = (int) Math.round(width * scale);
        int newHeight = (int) Math.round(height * scale);

        BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(original, 0, 0, newWidth, newHeight, null);
        g.dispose();
        return resized;
    }
}
