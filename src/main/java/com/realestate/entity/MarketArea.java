package com.realestate.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "market_areas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketArea {

    public enum Level {
        STATE, CITY, LOCALITY
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private MarketArea parent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Level level;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "state_name", length = 150)
    private String stateName;

    @Column(name = "city_name", length = 150)
    private String cityName;

    @Column(name = "state_slug", length = 160)
    private String stateSlug;

    @Column(name = "city_slug", length = 160)
    private String citySlug;

    @Column(name = "location_slug", length = 160)
    private String locationSlug;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private int sortOrder = 0;

    @CreationTimestamp
    @Column(name = "created_at")
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
