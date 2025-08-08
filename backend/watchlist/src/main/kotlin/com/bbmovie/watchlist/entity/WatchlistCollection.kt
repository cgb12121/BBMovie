package com.bbmovie.watchlist.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "watchlist_collection",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "name"])]
)
@EntityListeners(AuditingEntityListener::class)
class WatchlistCollection(
    @Id
    @GeneratedValue
    val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(name = "name", nullable = false, length = 100)
    var name: String,

    @Column(name = "description", length = 1000)
    var description: String? = null,

    @Column(name = "is_public")
    var isPublic: Boolean = false,

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) {

    // Required by Hibernate (for proxies, deserialization)
    constructor() : this(
        id = UUID.randomUUID(),
        userId = UUID.randomUUID(),
        name = ""
    )

    // Identity-based equals: only compare `id`
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        other as WatchlistCollection
        return id == other.id
    }

    // Identity-based hashCode: only use `id`
    override fun hashCode() = id.hashCode()

    override fun toString() = "WatchlistCollection(id=$id, name='$name', userId=$userId)"
}