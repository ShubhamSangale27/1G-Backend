package com.realestate.dto;

import com.realestate.entity.BlogContentBlock;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlogPostCreateUpdateRequest {
    @NotBlank
    private String title;
    private String excerpt;
    private String coverImageUrl;
    private String metaTitle;
    private String metaDescription;
    private String category;
    private String tags;
    private boolean published;
    @Valid
    @NotEmpty
    private List<BlockInput> blocks;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BlockInput {
        private BlogContentBlock.BlockType blockType;
        private String content;
        private String mediaUrl;
        private String linkUrl;
        private String caption;
        private Integer displayOrder;
    }
}

