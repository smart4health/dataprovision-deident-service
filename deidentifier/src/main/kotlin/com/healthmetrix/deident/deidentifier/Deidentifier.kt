package com.healthmetrix.deident.deidentifier

import com.github.michaelbull.result.partition
import com.healthmetrix.deident.commons.DateShiftedEvent
import com.healthmetrix.deident.commons.DeidentifiedEvent
import com.healthmetrix.deident.commons.ErrorEvent
import com.healthmetrix.deident.commons.tryPublishEvent
import com.healthmetrix.deident.deidentifier.jsonfilter.JsonFilterUseCase
import io.micrometer.core.annotation.Timed
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class Deidentifier(
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val jsonFilterUseCase: JsonFilterUseCase,
) {

    @EventListener
    @Timed(value = "s4h.deidentifier.timed", description = "Time taken for Deidentifier")
    fun onDateShiftedEvent(event: DateShiftedEvent) {
        val errors = event.transaction.all
            .map { jsonFilterUseCase(it, event.context.d4lId) }
            .partition()
            .second

        when {
            errors.isNotEmpty() -> ErrorEvent(event.context, errors)
            else -> DeidentifiedEvent(event.context, event.transaction)
        }.let(applicationEventPublisher::tryPublishEvent)
    }
}
