package com.realestate.dto;

import com.realestate.entity.UnmatchedFaqQuestion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnmatchedFaqQuestionDto {

    private Long id;
    private String questionText;
    private Long userId;
    private String userFullName;
    private String userEmail;
    private String status;
    private Long promotedFaqId;
    private Instant createdAt;
    private Instant resolvedAt;

    public static UnmatchedFaqQuestionDto from(UnmatchedFaqQuestion u) {
        if (u == null) return null;
        boolean hasUser = u.getUser() != null;
        return UnmatchedFaqQuestionDto.builder()
                .id(u.getId())
                .questionText(u.getQuestionText())
                .userId(hasUser ? u.getUser().getId() : null)
                .userFullName(hasUser ? u.getUser().getFullName() : "Unknown")
                .userEmail(hasUser ? u.getUser().getEmail() : null)
                .status(u.getStatus() != null ? u.getStatus().name() : null)
                .promotedFaqId(u.getPromotedFaq() != null ? u.getPromotedFaq().getId() : null)
                .createdAt(u.getCreatedAt())
                .resolvedAt(u.getResolvedAt())
                .build();
    }
}
