package com.healthmetrix.deident.statistics.usecase

import com.healthmetrix.deident.persistence.job.api.ResourceRepository
import org.springframework.stereotype.Component

@Component
class CountTotalResourcesUseCase(
    private val resourceRepository: ResourceRepository,
) {
    operator fun invoke(): Int = resourceRepository.countUploadedResources()
}
