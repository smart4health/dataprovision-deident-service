package com.healthmetrix.deident.download

import ca.uhn.fhir.context.FhirContext
import com.healthmetrix.deident.commons.JobId
import com.healthmetrix.deident.commons.Transaction
import com.healthmetrix.deident.commons.sha256
import com.healthmetrix.deident.persistence.job.api.Job
import com.healthmetrix.deident.persistence.job.api.JobRepository
import com.healthmetrix.deident.persistence.job.api.JobStatus
import com.healthmetrix.deident.persistence.job.api.Resource
import com.healthmetrix.deident.persistence.job.api.ResourceRepository
import org.hl7.fhir.r4.model.Bundle
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Component
class CreateJobUseCase(
    private val jobRepository: JobRepository,
    private val resourceRepository: ResourceRepository,
    private val fhirContext: FhirContext,
) {
    @Transactional
    operator fun invoke(userId: String, bundle: Bundle): Pair<Job, Transaction> {
        val jobId = JobId.randomUUID()

        val job = Job(
            id = jobId,
            userId = userId,
            status = JobStatus.Downloaded,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        ).also(jobRepository::save)

        val resourcesUploadedByUser = resourceRepository.findUploadedResourcesByUser(userId)

        var transaction = Transaction()
        bundle.entry?.map { entry ->
            entry to Resource(
                resourceId = entry.resource.id,
                jobId = jobId,
                hash = fhirContext.newJsonParser().encodeResourceToString(entry.resource).sha256(),
                rejectedReason = null,
            )
        }?.forEach { (entry, res) ->

            val uploaded = resourcesUploadedByUser.firstOrNull { it.resourceId == res.resourceId }
            when {
                uploaded == null -> transaction = transaction.create(entry.resource)
                uploaded.hash != res.hash -> transaction = transaction.update(entry.resource)
            }

            resourceRepository.save(res)
        }

        return job to transaction
    }
}
