package com.healthmetrix.deident.status.usecase

import com.healthmetrix.deident.persistence.job.api.Job
import com.healthmetrix.deident.persistence.job.api.JobRepository
import com.healthmetrix.deident.persistence.job.api.JobStatus
import org.springframework.stereotype.Component

@Component
class SearchJobsByUserUseCase(
    private val jobRepository: JobRepository,
) {
    operator fun invoke(userId: String): SearchResults {
        val (uploaded, rest) = jobRepository.findJobsByUserId(userId)
            .partition { it.status == JobStatus.Uploaded }

        val (errored, inProgress) = rest.partition { it.status == JobStatus.Error }

        return SearchResults(
            uploaded = uploaded.sortedByDescending { it.updatedAt },
            errored = errored.sortedByDescending { it.updatedAt },
            inProgress = inProgress.sortedByDescending { it.updatedAt },
        )
    }

    data class SearchResults(
        val uploaded: List<Job>,
        val errored: List<Job>,
        val inProgress: List<Job>,
    )
}
