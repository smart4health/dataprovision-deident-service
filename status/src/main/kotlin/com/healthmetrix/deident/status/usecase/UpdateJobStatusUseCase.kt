package com.healthmetrix.deident.status.usecase

import com.healthmetrix.deident.commons.JobId
import com.healthmetrix.deident.persistence.job.api.JobRepository
import com.healthmetrix.deident.persistence.job.api.JobStatus
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Component
class UpdateJobStatusUseCase(
    private val jobRepository: JobRepository,
) {

    @Transactional
    operator fun invoke(
        jobId: JobId,
        status: JobStatus,
        updatedAt: Instant = Instant.now(),
    ) {
        jobRepository.findJobById(jobId)
            ?.copy(status = status, updatedAt = updatedAt)
            ?.let(jobRepository::save)
    }
}
