package com.realestate.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "blog_content_blocks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlogContentBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private BlogPost post;

    @Enumerated(EnumType.STRING)
    @Column(name = "block_type", nullable = false, length = 20)
    private BlockType blockType;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(length = 1000)
    private String mediaUrl;

    @Column(length = 1000)
    private String linkUrl;

    @Column(length = 500)
    private String caption;

    @Column(nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    public enum BlockType {
        TEXT, IMAGE, VIDEO, LINK
    }
}

