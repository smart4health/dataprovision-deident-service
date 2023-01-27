package com.healthmetrix.deident.typefilter

import com.healthmetrix.deident.commons.DownloadedEvent
import com.healthmetrix.deident.commons.Transaction
import com.healthmetrix.deident.commons.TypeFilteredEvent
import com.healthmetrix.deident.commons.kv
import com.healthmetrix.deident.commons.logger
import com.healthmetrix.deident.commons.tryPublishEvent
import io.micrometer.core.annotation.Timed
import org.hl7.fhir.r4.model.AllergyIntolerance
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.Consent
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Immunization
import org.hl7.fhir.r4.model.MedicationStatement
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Procedure
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Resource
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

/**
 * Reject resources that are not used by the Research Platform
 */
@Component
class TypeFilter(
    private val typeFilterRejectResourceUseCase: TypeFilterRejectResourceUseCase,
    private val applicationEventPublisher: ApplicationEventPublisher,
) {
    @EventListener
    @Timed(value = "s4h.type-filter.timed", description = "Time taken to filter out resource types")
    fun onEvent(event: DownloadedEvent) {
        val (acceptedToUpdate, rejectedToUpdate) = event.transaction
            .toUpdate
            .partition(Resource::isAccepted)

        val (acceptedToCreate, rejectedToCreate) = event.transaction
            .toCreate
            .partition(Resource::isAccepted)

        rejectedToUpdate
            .plus(rejectedToCreate)
            .forEach { resource ->
                typeFilterRejectResourceUseCase(resource, event.context.jobId)

                logger.info(
                    "Type filtering resource {} {} {}",
                    "resourceType" kv resource.resourceType.toString(),
                    "resourceId" kv resource.id,
                    "jobId" kv event.context.jobId,
                )
            }

        val newTransaction = Transaction(
            toUpdate = acceptedToUpdate,
            toCreate = acceptedToCreate,
        )

        TypeFilteredEvent(event.context, newTransaction)
            .let(applicationEventPublisher::tryPublishEvent)
    }
}

private fun Resource.isAccepted(): Boolean = when (this) {
    is AllergyIntolerance,
    is Condition,
    is Consent,
    is Encounter,
    is Immunization,
    is MedicationStatement,
    is Observation,
    is Patient,
    is Procedure,
    is QuestionnaireResponse,
    -> true

    else -> false
}
