package com.realestate.dto;

import com.realestate.entity.BlogPost;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlogPostDto {
    private Long id;
    private String title;
    private String slug;
    private String excerpt;
    private String coverImageUrl;
    private String metaTitle;
    private String metaDescription;
    private String category;
    private String tags;
    private boolean published;
    private Instant publishedAt;
    private Instant createdAt;
    private Instant updatedAt;
    private Long authorId;
    private String authorName;
    private List<BlogContentBlockDto> blocks;

    public static BlogPostDto from(BlogPost p) {
        if (p == null) return null;
        return BlogPostDto.builder()
                .id(p.getId())
                .title(p.getTitle())
                .slug(p.getSlug())
                .excerpt(p.getExcerpt())
                .coverImageUrl(p.getCoverImageUrl())
                .metaTitle(p.getMetaTitle())
                .metaDescription(p.getMetaDescription())
                .category(p.getCategory())
                .tags(p.getTags())
                .published(p.isPublished())
                .publishedAt(p.getPublishedAt())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .authorId(p.getAuthor() != null ? p.getAuthor().getId() : null)
                .authorName(p.getAuthor() != null ? p.getAuthor().getFullName() : null)
                .blocks(p.getBlocks() == null ? List.of() : p.getBlocks().stream().map(BlogContentBlockDto::from).toList())
                .build();
    }
}

