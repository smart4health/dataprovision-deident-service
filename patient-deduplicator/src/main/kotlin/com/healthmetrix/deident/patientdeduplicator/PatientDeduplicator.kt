package com.healthmetrix.deident.patientdeduplicator

import com.healthmetrix.deident.commons.PatientDeduplicatedEvent
import com.healthmetrix.deident.commons.TypeFilteredEvent
import com.healthmetrix.deident.commons.tryPublishEvent
import io.micrometer.core.annotation.Timed
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

/**
 * Ensure at most one Patient resource is sent to the RP by
 * rejecting the Patient resources that are not marked as the
 * primary by the CHDP (with a `Patient.meta.tag` entry)
 */
@Component
class PatientDeduplicator(
    private val patientDeduplicatorUseCase: PatientDeduplicatorUseCase,
    private val applicationEventPublisher: ApplicationEventPublisher,
) {
    @EventListener
    @Timed(value = "s4h.patient-deduplicator.timed", description = "Time taken to filter out duplicate patients")
    fun onEvent(event: TypeFilteredEvent) {
        val newTransaction = patientDeduplicatorUseCase(event.transaction)

        PatientDeduplicatedEvent(event.context, newTransaction)
            .let(applicationEventPublisher::tryPublishEvent)
    }
}
