package com.healthmetrix.deident.persistence.resource

import com.healthmetrix.deident.commons.JobId
import com.healthmetrix.deident.persistence.job.api.JobStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import java.io.Serializable
import java.sql.Timestamp

@Entity
@Table(name = "resource")
@IdClass(ResourceCompositeKey::class)
data class ResourceEntity(
    @Id
    @Column(name = "id")
    val resourceId: String,

    @Id
    @Column(name = "job_id")
    val jobId: JobId,

    @Column(name = "hash")
    val hash: String,

    @Column(name = "rejected_reason")
    val rejectedReason: String?,
)

/**
 * an instance of a resource is uniquely identified by (job id x resource id)
 *
 * PURELY FOR HIBERNATE please do not use elsewhere
 */
data class ResourceCompositeKey(
    var resourceId: String? = null,
    var jobId: JobId? = null,
) : Serializable // for hibernate

@Entity
@Immutable
@Table(name = "resource_with_job")
@IdClass(ResourceCompositeKey::class)
data class ResourceWithJobEntity(
    @Id
    @Column(name = "resource_id")
    val resourceId: String,

    @Column(name = "hash")
    val hash: String,

    @Id
    @Column(name = "job_id")
    val jobId: JobId,

    @Column(name = "user_id")
    val userId: String,

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    val status: JobStatus,

    @Column(name = "created_at")
    val createdAt: Timestamp,

    @Column(name = "updated_at")
    val updatedAt: Timestamp,

    @Column(name = "rejected_reason")
    val rejectedReason: String?,
)
