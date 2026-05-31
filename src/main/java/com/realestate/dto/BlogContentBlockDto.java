package com.realestate.dto;

import com.realestate.entity.BlogContentBlock;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlogContentBlockDto {
    private Long id;
    private BlogContentBlock.BlockType blockType;
    private String content;
    private String mediaUrl;
    private String linkUrl;
    private String caption;
    private Integer displayOrder;

    public static BlogContentBlockDto from(BlogContentBlock b) {
        if (b == null) return null;
        return BlogContentBlockDto.builder()
                .id(b.getId())
                .blockType(b.getBlockType())
                .content(b.getContent())
                .mediaUrl(b.getMediaUrl())
                .linkUrl(b.getLinkUrl())
                .caption(b.getCaption())
                .displayOrder(b.getDisplayOrder())
                .build();
    }
}

