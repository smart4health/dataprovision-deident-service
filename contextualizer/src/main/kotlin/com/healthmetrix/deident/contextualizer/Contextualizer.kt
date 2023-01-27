package com.healthmetrix.deident.contextualizer

import com.healthmetrix.deident.commons.ContextualizedEvent
import com.healthmetrix.deident.commons.PatientDeduplicatedEvent
import com.healthmetrix.deident.commons.tryPublishEvent
import io.micrometer.core.annotation.Timed
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class Contextualizer(
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val contextualizeUseCase: ContextualizeUseCase,
) {

    @EventListener
    @Timed(value = "s4h.contextualizer.timed", description = "Time taken for Contextualizer")
    fun onEvent(event: PatientDeduplicatedEvent) {
        event.transaction.all.forEach {
            contextualizeUseCase(it, event.context.d4lId)
        }

        ContextualizedEvent(event.context, event.transaction)
            .let(applicationEventPublisher::tryPublishEvent)
    }
}
