package com.realestate.service;

import com.realestate.dto.BlogFiltersDto;
import com.realestate.dto.BlogPostCreateUpdateRequest;
import com.realestate.dto.BlogPostDto;
import com.realestate.dto.PageResponse;
import com.realestate.entity.BlogContentBlock;
import com.realestate.entity.BlogPost;
import com.realestate.entity.User;
import com.realestate.exception.BadRequestException;
import com.realestate.exception.ResourceNotFoundException;
import com.realestate.repository.BlogPostRepository;
import com.realestate.repository.UserRepository;
import com.realestate.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Arrays;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BlogService {
    private final BlogPostRepository blogPostRepository;
    private final UserRepository userRepository;

    public PageResponse<BlogPostDto> getPublished(int page, int size, String category, String tag) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "publishedAt"));
        boolean hasCategory = category != null && !category.isBlank();
        boolean hasTag = tag != null && !tag.isBlank();
        Page<BlogPost> result = (hasCategory || hasTag)
                ? blogPostRepository.findPublishedFiltered(
                        hasCategory ? category.trim() : null,
                        hasTag ? tag.trim() : null,
                        pageable)
                : blogPostRepository.findByPublishedTrue(pageable);
        return toPage(result);
    }

    public BlogFiltersDto getPublishedFilters() {
        List<String> categories = blogPostRepository.findDistinctPublishedCategories();
        Set<String> tagSet = new LinkedHashSet<>();
        for (String raw : blogPostRepository.findPublishedTagStrings()) {
            if (raw == null || raw.isBlank()) continue;
            Arrays.stream(raw.split(","))
                    .map(String::trim)
                    .filter(t -> !t.isEmpty())
                    .forEach(tagSet::add);
        }
        List<String> tags = tagSet.stream().sorted(String.CASE_INSENSITIVE_ORDER).collect(Collectors.toList());
        return BlogFiltersDto.builder().categories(categories).tags(tags).build();
    }

    public BlogPostDto getPublishedBySlug(String slug) {
        BlogPost post = blogPostRepository.findBySlugAndPublishedTrue(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Blog post not found with slug: " + slug));
        return BlogPostDto.from(post);
    }

    public PageResponse<BlogPostDto> getEditorPosts(int page, int size, UserPrincipal principal) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<BlogPost> result = isAdmin(principal)
                ? blogPostRepository.findAll(pageable)
                : blogPostRepository.findByAuthorId(principal.getId(), pageable);
        return toPage(result);
    }

    @Transactional
    public BlogPostDto create(BlogPostCreateUpdateRequest request, UserPrincipal principal) {
        User author = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", principal.getId()));
        BlogPost post = BlogPost.builder()
                .title(request.getTitle().trim())
                .slug(generateUniqueSlug(request.getTitle(), null))
                .excerpt(trimToNull(request.getExcerpt()))
                .coverImageUrl(trimToNull(request.getCoverImageUrl()))
                .metaTitle(trimToNull(request.getMetaTitle()))
                .metaDescription(trimToNull(request.getMetaDescription()))
                .category(trimToNull(request.getCategory()))
                .tags(normalizeTags(request.getTags()))
                .published(request.isPublished())
                .publishedAt(request.isPublished() ? Instant.now() : null)
                .author(author)
                .build();
        syncBlocks(post, request);
        return BlogPostDto.from(blogPostRepository.save(post));
    }

    @Transactional
    public BlogPostDto update(Long id, BlogPostCreateUpdateRequest request, UserPrincipal principal) {
        BlogPost post = blogPostRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Blog post", id));
        requireEditorAccess(post, principal);
        post.setTitle(request.getTitle().trim());
        post.setSlug(generateUniqueSlug(request.getTitle(), post.getId()));
        post.setExcerpt(trimToNull(request.getExcerpt()));
        post.setCoverImageUrl(trimToNull(request.getCoverImageUrl()));
        post.setMetaTitle(trimToNull(request.getMetaTitle()));
        post.setMetaDescription(trimToNull(request.getMetaDescription()));
        post.setCategory(trimToNull(request.getCategory()));
        post.setTags(normalizeTags(request.getTags()));
        boolean publishingNow = request.isPublished() && !post.isPublished();
        post.setPublished(request.isPublished());
        if (request.isPublished()) {
            post.setPublishedAt(publishingNow ? Instant.now() : (post.getPublishedAt() == null ? Instant.now() : post.getPublishedAt()));
        } else {
            post.setPublishedAt(null);
        }
        syncBlocks(post, request);
        return BlogPostDto.from(blogPostRepository.save(post));
    }

    @Transactional
    public BlogPostDto setPublished(Long id, boolean published, UserPrincipal principal) {
        BlogPost post = blogPostRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Blog post", id));
        requireEditorAccess(post, principal);
        post.setPublished(published);
        post.setPublishedAt(published ? (post.getPublishedAt() == null ? Instant.now() : post.getPublishedAt()) : null);
        return BlogPostDto.from(blogPostRepository.save(post));
    }

    @Transactional
    public void delete(Long id, UserPrincipal principal) {
        BlogPost post = blogPostRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Blog post", id));
        requireEditorAccess(post, principal);
        blogPostRepository.delete(post);
    }

    private void syncBlocks(BlogPost post, BlogPostCreateUpdateRequest request) {
        if (request.getBlocks() == null || request.getBlocks().isEmpty()) {
            throw new BadRequestException("At least one content block is required.");
        }
        post.getBlocks().clear();
        List<BlogPostCreateUpdateRequest.BlockInput> sorted = request.getBlocks().stream()
                .sorted(Comparator.comparingInt(b -> b.getDisplayOrder() == null ? 0 : b.getDisplayOrder()))
                .toList();
        int idx = 0;
        for (BlogPostCreateUpdateRequest.BlockInput b : sorted) {
            BlogContentBlock.BlockType type = b.getBlockType();
            if (type == null) {
                throw new BadRequestException("Each block requires blockType.");
            }
            if (type == BlogContentBlock.BlockType.TEXT && isHtmlBlank(b.getContent())) {
                throw new BadRequestException("TEXT block requires content.");
            }
            if ((type == BlogContentBlock.BlockType.IMAGE || type == BlogContentBlock.BlockType.VIDEO) && isBlank(b.getMediaUrl())) {
                throw new BadRequestException(type + " block requires mediaUrl.");
            }
            if (type == BlogContentBlock.BlockType.LINK && (isBlank(b.getLinkUrl()) || isBlank(b.getContent()))) {
                throw new BadRequestException("LINK block requires linkUrl and content.");
            }
            BlogContentBlock block = BlogContentBlock.builder()
                    .post(post)
                    .blockType(type)
                    .content(trimToNull(b.getContent()))
                    .mediaUrl(trimToNull(b.getMediaUrl()))
                    .linkUrl(trimToNull(b.getLinkUrl()))
                    .caption(trimToNull(b.getCaption()))
                    .displayOrder(b.getDisplayOrder() != null ? b.getDisplayOrder() : idx)
                    .build();
            post.getBlocks().add(block);
            idx++;
        }
    }

    private void requireEditorAccess(BlogPost post, UserPrincipal principal) {
        if (isAdmin(principal)) return;
        if (!post.getAuthor().getId().equals(principal.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Not allowed");
        }
    }

    private boolean isAdmin(UserPrincipal principal) {
        return "ADMIN".equals(principal.getRole());
    }

    private PageResponse<BlogPostDto> toPage(Page<BlogPost> page) {
        return PageResponse.<BlogPostDto>builder()
                .content(page.getContent().stream().map(BlogPostDto::from).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    private String generateUniqueSlug(String title, Long currentId) {
        String base = slugify(title);
        String candidate = base;
        int n = 2;
        while (true) {
            var existing = blogPostRepository.findBySlug(candidate);
            if (existing.isEmpty() || (currentId != null && existing.get().getId().equals(currentId))) {
                return candidate;
            }
            candidate = base + "-" + n++;
        }
    }

    private String slugify(String input) {
        String s = input == null ? "" : input.toLowerCase(Locale.ROOT).trim();
        s = s.replaceAll("[^a-z0-9\\s-]", "");
        s = s.replaceAll("\\s+", "-");
        s = s.replaceAll("-{2,}", "-");
        s = s.replaceAll("^-|-$", "");
        return s.isBlank() ? "post" : s;
    }

    private String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private boolean isHtmlBlank(String html) {
        if (isBlank(html)) return true;
        String text = html.replaceAll("<[^>]*>", "")
                .replace("&nbsp;", " ")
                .trim();
        return text.isEmpty();
    }

    private String normalizeTags(String tags) {
        if (tags == null || tags.isBlank()) return null;
        String normalized = Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(t -> !t.isEmpty())
                .map(t -> t.toLowerCase(Locale.ROOT))
                .distinct()
                .collect(Collectors.joining(","));
        return normalized.isEmpty() ? null : normalized;
    }
}

