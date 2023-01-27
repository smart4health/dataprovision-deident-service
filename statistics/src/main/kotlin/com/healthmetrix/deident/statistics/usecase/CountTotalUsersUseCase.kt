package com.healthmetrix.deident.statistics.usecase

import com.healthmetrix.deident.persistence.job.api.JobRepository
import org.springframework.stereotype.Component

@Component
class CountTotalUsersUseCase(
    private val jobRepository: JobRepository,
) {
    operator fun invoke(): Int = jobRepository.countAllUsers()
}
