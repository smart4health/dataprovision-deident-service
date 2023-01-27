package com.healthmetrix.deident.contextualizer

import org.hl7.fhir.r4.model.AllergyIntolerance
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.Consent
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Immunization
import org.hl7.fhir.r4.model.MedicationStatement
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Procedure
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.Resource
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class ContextualizeUseCase(
    @Value("\${contextualizer.identifier-system}")
    private val identifierSystem: String,
) {
    operator fun invoke(resource: Resource, d4lId: String) {
        val ref = Reference().apply {
            identifier = Identifier().apply {
                value = d4lId
                system = identifierSystem
            }
        }

        when (resource) {
            is AllergyIntolerance -> resource.patient = resource.patient.orElse(ref)
            is Condition -> resource.subject = resource.subject.orElse(ref)
            is Consent -> resource.patient = resource.patient.orElse(ref)
            is Encounter -> resource.subject = resource.subject.orElse(ref)
            is Immunization -> resource.patient = resource.patient.orElse(ref)
            is MedicationStatement -> resource.subject = resource.subject.orElse(ref)
            is Observation -> resource.subject = resource.subject.orElse(ref)
            is Patient -> resource.identifier.add(ref.identifier)
            is Procedure -> resource.subject = resource.subject.orElse(ref)
            is QuestionnaireResponse -> resource.subject = resource.subject.orElse(ref)
            else -> Unit
        }
    }

    private fun Reference.orElse(ref: Reference): Reference = when {
        hasReference() -> this
        hasIdentifier() -> this
        else -> ref
    }
}
