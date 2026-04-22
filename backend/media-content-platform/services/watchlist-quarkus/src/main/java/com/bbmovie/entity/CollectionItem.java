package com.bbmovie.entity;

import com.bbmovie.entity.enums.WatchStatus;
import com.bbmovie.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "collection_item", uniqueConstraints = @UniqueConstraint(columnNames = {"collection_id", "movie_id"}))
public class CollectionItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_id", nullable = false)
    private WatchlistCollection collection;

    @Column(name = "movie_id", nullable = false)
    private UUID movieId;

    @Column(name = "added_at", nullable = false)
    private Instant addedAt = Instant.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "watch_status", nullable = false, length = 20)
    private WatchStatus watchStatus = WatchStatus.PLANNING;

    @Column(name = "watched_at")
    private Instant watchedAt;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;
}


