package com.bbmovie.watchlist.entity

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant
import java.util.*

@Entity
@Table(
    name = "collection_item",
    uniqueConstraints = [UniqueConstraint(columnNames = ["collection_id", "movie_id"])]
)
@EntityListeners(AuditingEntityListener::class)
open class CollectionItem(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_id", nullable = false)
    val collection: WatchlistCollection,

    @Column(name = "movie_id", nullable = false)
    val movieId: UUID,

    @CreatedDate
    @Column(name = "added_at", nullable = false)
    val addedAt: Instant = Instant.now(),

    @Column(name = "watch_status", nullable = false, length = 20)
    var watchStatus: WatchStatus = WatchStatus.PLANNING,

    @Column(name = "watched_at")
    var watchedAt: Instant? = null,

    @Column(name = "notes", length = 1000)
    var notes: String? = null,

    @Column(name = "sort_order")
    var sortOrder: Int = 0
) {

    // No-arg constructor required by Hibernate (for proxying, reflection)
    constructor() : this(
        id = UUID.randomUUID(),
        collection = WatchlistCollection(), // placeholder
        movieId = UUID.randomUUID()
    )

    // Identity-based equality: only compare `id`
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        other as CollectionItem
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String =
        "CollectionItem(" +
                "id=$id, " +
                "collectionId=${collection.id}, " +
                "movieId=$movieId, " +
                "watchStatus=$watchStatus, " +
                "addedAt=$addedAt" +
                ")"
}