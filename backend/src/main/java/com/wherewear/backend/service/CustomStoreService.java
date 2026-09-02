package com.wherewear.backend.service;

import com.wherewear.backend.dto.CustomStoreDtos.CustomStoreRequest;
import com.wherewear.backend.dto.CustomStoreDtos.CustomStoreResponse;
import com.wherewear.backend.model.CustomStore;
import com.wherewear.backend.repository.CustomStoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class CustomStoreService {

    private final CustomStoreRepository customStoreRepository;

    public CustomStoreService(CustomStoreRepository customStoreRepository) {
        this.customStoreRepository = customStoreRepository;
    }

    public List<CustomStoreResponse> listForUser(String userId) {
        return customStoreRepository.findAllForUser(userId).stream()
                .map(CustomStoreService::toResponse)
                .toList();
    }

    public CustomStoreResponse create(String userId, CustomStoreRequest request) {
        CustomStore store = new CustomStore();
        store.setUserId(userId);
        store.setName(request.name());
        store.setUrl(blankToNull(request.url()));
        return toResponse(customStoreRepository.save(store));
    }

    public void delete(String userId, String storeId) {
        CustomStore store = customStoreRepository.findById(storeId);
        if (store == null) {
            throw new ResponseStatusException(NOT_FOUND, "Store not found");
        }
        if (!store.getUserId().equals(userId)) {
            throw new ResponseStatusException(FORBIDDEN, "Not your store");
        }
        customStoreRepository.deleteById(storeId);
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    private static CustomStoreResponse toResponse(CustomStore store) {
        return new CustomStoreResponse(store.getId(), store.getName(), store.getUrl());
    }
}
