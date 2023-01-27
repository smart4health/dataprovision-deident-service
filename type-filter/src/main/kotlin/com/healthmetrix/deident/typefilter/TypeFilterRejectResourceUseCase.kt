package com.healthmetrix.deident.typefilter

import com.healthmetrix.deident.commons.JobId
import com.healthmetrix.deident.commons.kv
import com.healthmetrix.deident.commons.logger
import com.healthmetrix.deident.persistence.job.api.ResourceRepository
import org.hl7.fhir.r4.model.Resource
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class TypeFilterRejectResourceUseCase(
    private val resourceRepository: ResourceRepository,
) {
    @Transactional
    operator fun invoke(
        resource: Resource,
        jobId: JobId,
    ) {
        resourceRepository.findByIdAndJob(resource.id, jobId)
            ?.copy(rejectedReason = "Resource type not accepted by RP")
            ?.let(resourceRepository::save)
            ?: run {
                logger.warn(
                    "Resource to reject not found {}",
                    "jobId" kv jobId,
                    "resourceId" kv resource.id,
                )
            }
    }
}
