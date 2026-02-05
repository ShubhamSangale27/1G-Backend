package com.realestate.dto;

import com.realestate.entity.SiteVisitComment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SiteVisitCommentDto {

    private Long id;
    private Long userId;
    private String userName;
    private String commentText;
    private Instant createdAt;

    public static SiteVisitCommentDto from(SiteVisitComment c) {
        if (c == null) return null;
        return SiteVisitCommentDto.builder()
                .id(c.getId())
                .userId(c.getUser() != null ? c.getUser().getId() : null)
                .userName(c.getUser() != null ? c.getUser().getFullName() : null)
                .commentText(c.getCommentText())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
