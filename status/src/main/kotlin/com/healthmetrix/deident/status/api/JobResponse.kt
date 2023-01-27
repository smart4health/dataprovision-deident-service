package com.healthmetrix.deident.status.api

import com.healthmetrix.deident.persistence.job.api.Job
import com.healthmetrix.deident.persistence.job.api.JobStatus
import java.time.Instant

/**
 * for serializing to json
 *
 * Instants are serialized to IS0 8601 in UTC
 */
data class JobResponse(
    val status: JobStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
)

internal fun Job.asResponse() = JobResponse(
    status = status,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
