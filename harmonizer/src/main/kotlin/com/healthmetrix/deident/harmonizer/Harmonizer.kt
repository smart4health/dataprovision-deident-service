package com.healthmetrix.deident.harmonizer

import com.github.michaelbull.result.partition
import com.healthmetrix.deident.commons.DeidentifiedEvent
import com.healthmetrix.deident.commons.ErrorEvent
import com.healthmetrix.deident.commons.HarmonizedEvent
import com.healthmetrix.deident.commons.tryPublishEvent
import com.healthmetrix.deident.harmonizer.harmonize.ResourceHarmonizeUseCase
import io.micrometer.core.annotation.Timed
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class Harmonizer(
    private val resourceHarmonizeUseCase: ResourceHarmonizeUseCase,
    private val applicationEventPublisher: ApplicationEventPublisher,
) {
    @EventListener
    @Timed(value = "s4h.harmonizer.timed", description = "Time taken for Harmonizer")
    fun onDeidentifiedEvent(event: DeidentifiedEvent) {
        val errors = event.transaction.all
            .map(resourceHarmonizeUseCase::invoke)
            .partition()
            .second

        when {
            errors.isNotEmpty() -> ErrorEvent(event.context, errors)
            else -> HarmonizedEvent(event.context, event.transaction)
        }.let(applicationEventPublisher::tryPublishEvent)
    }
}
