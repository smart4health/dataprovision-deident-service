package com.healthmetrix.deident.dateshifter

import com.healthmetrix.deident.commons.JobId
import com.healthmetrix.deident.commons.kv
import com.healthmetrix.deident.commons.logger
import com.healthmetrix.deident.persistence.job.api.ResourceRepository
import org.hl7.fhir.r4.model.Resource
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class RejectResourceUseCase(
    private val resourceRepository: ResourceRepository,
) {
    @Transactional
    operator fun invoke(
        resource: Resource,
        jobId: JobId,
        outcome: ShiftDateUseCase.Outcome,
    ) {
        val reason = when (outcome) {
            ShiftDateUseCase.Outcome.KEEP -> return
            ShiftDateUseCase.Outcome.TRUNCATE_START -> "TruncateStart"
            ShiftDateUseCase.Outcome.TRUNCATE_END -> "TruncateEnd"
        }

        resourceRepository.findByIdAndJob(resource.id, jobId)
            ?.copy(rejectedReason = reason)
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
