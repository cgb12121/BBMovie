package com.bbmovie.payment.entity.base

import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.Version
import lombok.Getter
import lombok.Setter
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime
import java.util.*

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
@Getter
@Setter
abstract class BaseEntity {
    @Id
    private val id: UUID? = null

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private val createdDate: LocalDateTime? = null

    @LastModifiedDate
    @Column(nullable = false)
    private val lastModifiedDate: LocalDateTime? = null

    @Version
    private val version: Long? = null
}