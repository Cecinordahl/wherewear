package com.wherewear.backend.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InventoryItemPhotoValidationTest {

    @Test
    void blankPhotoIsStoredAsNull() {
        assertThat(InventoryItemService.validatePhotoDataUrl(null)).isNull();
        assertThat(InventoryItemService.validatePhotoDataUrl("")).isNull();
    }

    @Test
    void acceptsJpegDataUrl() {
        String dataUrl = "data:image/jpeg;base64,/9j/4AAQSkZJRg==";
        assertThat(InventoryItemService.validatePhotoDataUrl(dataUrl)).isEqualTo(dataUrl);
    }

    @Test
    void rejectsNonJpegOrRemoteUrls() {
        assertThatThrownBy(() -> InventoryItemService.validatePhotoDataUrl("data:image/svg+xml;base64,PHN2Zz4="))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> InventoryItemService.validatePhotoDataUrl("https://example.com/a.jpg"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void rejectsInvalidBase64() {
        assertThatThrownBy(() -> InventoryItemService.validatePhotoDataUrl("data:image/jpeg;base64,not*base64"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void rejectsOversizedPhoto() {
        String huge = "data:image/jpeg;base64," + "A".repeat(800_000);
        assertThatThrownBy(() -> InventoryItemService.validatePhotoDataUrl(huge))
                .isInstanceOf(ResponseStatusException.class);
    }
}
