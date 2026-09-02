package com.wherewear.backend.controller;

import com.wherewear.backend.dto.SearchDtos.SearchResult;
import com.wherewear.backend.security.RequestUserContext;
import com.wherewear.backend.service.SearchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    public List<SearchResult> search(@RequestParam String q) {
        return searchService.search(RequestUserContext.requireUserId(), q);
    }
}
