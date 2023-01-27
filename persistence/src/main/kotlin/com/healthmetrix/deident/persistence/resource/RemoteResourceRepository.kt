package com.healthmetrix.deident.persistence.resource

import com.healthmetrix.deident.commons.JobId
import com.healthmetrix.deident.persistence.job.api.JobStatus
import com.healthmetrix.deident.persistence.job.api.Resource
import com.healthmetrix.deident.persistence.job.api.ResourceRepository
import org.springframework.context.annotation.Profile
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Component

@Component
@Profile("rds | postgres")
class RemoteResourceRepository(
    private val remoteResourceCrudRepository: RemoteResourceCrudRepository,
    private val remoteResourceWithJobCrudRepository: RemoteResourceWithJobCrudRepository,
) : ResourceRepository {
    override fun save(resource: Resource) {
        remoteResourceCrudRepository.save(
            ResourceEntity(
                resourceId = resource.resourceId,
                jobId = resource.jobId,
                hash = resource.hash,
                rejectedReason = resource.rejectedReason,
            ),
        )
    }

    // could move more logic into sql
    override fun findUploadedResourcesByUser(userId: String): List<Resource> {
        return remoteResourceWithJobCrudRepository
            .findUploadedResourcesByUserAndStatus(userId, JobStatus.Uploaded)
            .groupBy(ResourceWithJobEntity::resourceId)
            .mapNotNull { (_, r) ->
                r.maxByOrNull { it.updatedAt } // get latest uploaded only
            }.map { it.toResource() }
    }

    override fun findByIdAndJob(resourceId: String, jobId: JobId): Resource? =
        remoteResourceCrudRepository.findByIdAndJob(resourceId, jobId)?.let {
            Resource(
                resourceId = it.resourceId,
                jobId = it.jobId,
                hash = it.hash,
                rejectedReason = it.rejectedReason,
            )
        }

    override fun findFirstRejectedResourceByUser(userId: String): Resource? {
        return remoteResourceWithJobCrudRepository.findRejectedResourcesByUser(userId)
            .firstOrNull()
            ?.toResource()
    }

    override fun countUploadedResources(): Int {
        return remoteResourceWithJobCrudRepository
            .findUploadedResourcesByStatus(JobStatus.Uploaded)
            .groupBy(ResourceWithJobEntity::resourceId)
            .size
    }

    private fun ResourceWithJobEntity.toResource(): Resource {
        return Resource(
            resourceId = resourceId,
            jobId = jobId,
            hash = hash,
            rejectedReason = rejectedReason,
        )
    }
}

@Profile("rds | postgres")
interface RemoteResourceCrudRepository : CrudRepository<ResourceEntity, ResourceCompositeKey> {
    @Query("SELECT r from ResourceEntity r WHERE r.resourceId = :resourceId AND r.jobId = :jobId")
    fun findByIdAndJob(resourceId: String, jobId: JobId): ResourceEntity?
}

@Profile("rds | postgres")
interface RemoteResourceWithJobCrudRepository : CrudRepository<ResourceWithJobEntity, ResourceCompositeKey> {

    @Query("SELECT r from ResourceWithJobEntity r WHERE r.status = :jobStatus")
    fun findUploadedResourcesByStatus(jobStatus: JobStatus): List<ResourceWithJobEntity>

    @Query("SELECT r from ResourceWithJobEntity r WHERE r.userId = :userId AND r.status = :jobStatus")
    fun findUploadedResourcesByUserAndStatus(userId: String, jobStatus: JobStatus): List<ResourceWithJobEntity>

    @Query("SELECT r from ResourceWithJobEntity r WHERE r.userId = :userId AND r.rejectedReason IS NOT NULL ORDER BY r.createdAt")
    fun findRejectedResourcesByUser(userId: String): List<ResourceWithJobEntity>
}
