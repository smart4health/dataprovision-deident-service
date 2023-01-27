package com.healthmetrix.deident.status.usecase

import com.healthmetrix.deident.persistence.job.api.Job
import com.healthmetrix.deident.persistence.job.api.JobRepository
import com.healthmetrix.deident.persistence.job.api.ResourceRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class FindFirstRejectedJobUseCase(
    private val resourceRepository: ResourceRepository,
    private val jobRepository: JobRepository,
) {

    @Transactional
    operator fun invoke(userId: String): Job? = resourceRepository
        .findFirstRejectedResourceByUser(userId)
        ?.jobId
        ?.let(jobRepository::findJobById)
}
