package com.realestate.dto;

import com.realestate.entity.Alert;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertDto {

    private Long id;
    private String title;
    private String message;
    private boolean read;
    private String type;
    private Long referenceId;
    private Instant createdAt;

    public static AlertDto from(Alert a) {
        if (a == null) return null;
        return AlertDto.builder()
                .id(a.getId())
                .title(a.getTitle())
                .message(a.getMessage())
                .read(a.isRead())
                .type(a.getType())
                .referenceId(a.getReferenceId())
                .createdAt(a.getCreatedAt())
                .build();
    }
}
