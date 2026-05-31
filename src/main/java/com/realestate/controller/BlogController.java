package com.realestate.controller;

import com.realestate.dto.BlogFiltersDto;
import com.realestate.dto.BlogPostCreateUpdateRequest;
import com.realestate.dto.BlogPostDto;
import com.realestate.dto.PageResponse;
import com.realestate.security.UserPrincipal;
import com.realestate.service.BlogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/blogs")
@RequiredArgsConstructor
public class BlogController {
    private final BlogService blogService;

    @GetMapping("/published")
    public ResponseEntity<PageResponse<BlogPostDto>> getPublished(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String tag) {
        return ResponseEntity.ok(blogService.getPublished(page, size, category, tag));
    }

    @GetMapping("/published/filters")
    public ResponseEntity<BlogFiltersDto> getPublishedFilters() {
        return ResponseEntity.ok(blogService.getPublishedFilters());
    }

    @GetMapping("/published/{slug}")
    public ResponseEntity<BlogPostDto> getPublishedBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(blogService.getPublishedBySlug(slug));
    }

    @GetMapping("/editor/mine")
    public ResponseEntity<PageResponse<BlogPostDto>> getEditorPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(blogService.getEditorPosts(page, size, principal));
    }

    @PostMapping("/editor")
    public ResponseEntity<BlogPostDto> create(
            @Valid @RequestBody BlogPostCreateUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(blogService.create(request, principal));
    }

    @PutMapping("/editor/{id}")
    public ResponseEntity<BlogPostDto> update(
            @PathVariable Long id,
            @Valid @RequestBody BlogPostCreateUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(blogService.update(id, request, principal));
    }

    @PutMapping("/editor/{id}/publish")
    public ResponseEntity<BlogPostDto> setPublished(
            @PathVariable Long id,
            @RequestParam boolean published,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(blogService.setPublished(id, published, principal));
    }

    @DeleteMapping("/editor/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        blogService.delete(id, principal);
        return ResponseEntity.noContent().build();
    }
}

