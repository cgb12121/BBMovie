package com.bbmovie.payment.entity.base

import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.Version
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime
import java.util.*

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseEntity {

    @Id
    @GeneratedValue
    var id: UUID? = null

    @CreatedDate
    @Column(nullable = false, updatable = false)
    var createdDate: LocalDateTime? = null

    @LastModifiedDate
    @Column(nullable = false)
    var lastModifiedDate: LocalDateTime? = null

    @Version
    var version: Long? = null
}