package com.realestate.dto;

import com.realestate.entity.Faq;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FaqDto {

    private Long id;
    private String question;
    private String answer;
    private String keywords;
    private boolean active;
    private int sortOrder;
    private Instant createdAt;
    private Instant updatedAt;

    public static FaqDto from(Faq faq) {
        if (faq == null) return null;
        return FaqDto.builder()
                .id(faq.getId())
                .question(faq.getQuestion())
                .answer(faq.getAnswer())
                .keywords(faq.getKeywords())
                .active(faq.isActive())
                .sortOrder(faq.getSortOrder())
                .createdAt(faq.getCreatedAt())
                .updatedAt(faq.getUpdatedAt())
                .build();
    }
}
