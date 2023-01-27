package com.healthmetrix.deident.persistence.resource

import com.healthmetrix.deident.commons.JobId
import com.healthmetrix.deident.persistence.job.api.Job
import com.healthmetrix.deident.persistence.job.api.JobRepository
import com.healthmetrix.deident.persistence.job.api.JobStatus
import com.healthmetrix.deident.persistence.job.api.Resource
import com.healthmetrix.deident.persistence.job.api.ResourceRepository
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@Profile("!rds & !postgres")
class InMemoryResourceRepository(
    private val jobRepository: JobRepository,
) : ResourceRepository {

    private val map: MutableMap<String, Resource> = mutableMapOf()

    override fun save(resource: Resource) {
        map[UUID.randomUUID().toString()] = resource
    }

    override fun findUploadedResourcesByUser(userId: String): List<Resource> = map.joinOnJobId(jobRepository)
        .filter { (j, _) ->
            j.userId == userId && j.status == JobStatus.Uploaded
        }.groupBy { (_, r) ->
            r.resourceId
        }.mapNotNull { (_, pairs) ->
            pairs.maxByOrNull { (j, _) -> j.updatedAt }
        }.map(Pair<Job, Resource>::second)

    override fun findByIdAndJob(resourceId: String, jobId: JobId): Resource? =
        map.entries.find { (_, r) ->
            r.resourceId == resourceId && r.jobId == jobId
        }?.value

    override fun findFirstRejectedResourceByUser(userId: String): Resource? =
        map.joinOnJobId(jobRepository)
            .filter { (_, r) -> r.rejectedReason != null }
            .minByOrNull { (j, _) -> j.createdAt }
            ?.second

    override fun countUploadedResources(): Int = map.joinOnJobId(jobRepository)
        .filter { (j, _) -> j.status == JobStatus.Uploaded }
        .groupBy { (_, r) -> r.resourceId }.size

    private fun <T> Map<T, Resource>.joinOnJobId(jobRepository: JobRepository): List<Pair<Job, Resource>> = toList()
        .mapNotNull { (_, r) ->
            r.jobId.let(jobRepository::findJobById)?.to(r)
        }
}
