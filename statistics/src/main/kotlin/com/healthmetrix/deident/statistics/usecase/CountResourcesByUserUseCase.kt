package com.healthmetrix.deident.statistics.usecase

import com.healthmetrix.deident.persistence.job.api.ResourceRepository
import org.springframework.stereotype.Component

@Component
class CountResourcesByUserUseCase(
    private val resourceRepository: ResourceRepository,
) {
    operator fun invoke(userId: String): Int = resourceRepository.findUploadedResourcesByUser(userId).size
}
