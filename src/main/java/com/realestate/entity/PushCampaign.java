package com.realestate.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "push_campaigns")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PushCampaign {

    public enum LinkTarget {
        APP, EXTERNAL
    }

    public enum TargetRole {
        ALL, USER, AGENT
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 2000)
    private String body;

    @Column(name = "image_url", length = 2048)
    private String imageUrl;

    @Column(name = "link_url", length = 2048)
    private String linkUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "link_target", nullable = false, length = 16)
    @Builder.Default
    private LinkTarget linkTarget = LinkTarget.APP;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_role", nullable = false, length = 16)
    @Builder.Default
    private TargetRole targetRole = TargetRole.ALL;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sent_by")
    private User sentBy;

    @Column(name = "sent_count", nullable = false)
    @Builder.Default
    private int sentCount = 0;

    @Column(name = "failed_count", nullable = false)
    @Builder.Default
    private int failedCount = 0;

    @CreationTimestamp
    private Instant createdAt;
}
