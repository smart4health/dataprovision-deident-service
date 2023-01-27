package com.healthmetrix.deident.persistence.job.api

import com.healthmetrix.deident.commons.JobId

/**
 * Represents a resource ingested by the downloader
 */
data class Resource(
    val resourceId: String,
    val jobId: JobId,
    val hash: String,
    val rejectedReason: String?,
)

interface ResourceRepository {
    fun save(resource: Resource)

    fun findUploadedResourcesByUser(userId: String): List<Resource>

    fun findByIdAndJob(resourceId: String, jobId: JobId): Resource?

    fun findFirstRejectedResourceByUser(userId: String): Resource?

    fun countUploadedResources(): Int
}
