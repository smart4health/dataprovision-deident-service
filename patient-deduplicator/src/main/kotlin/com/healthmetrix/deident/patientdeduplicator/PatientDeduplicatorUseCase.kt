package com.healthmetrix.deident.patientdeduplicator

import com.healthmetrix.deident.commons.Transaction
import org.hl7.fhir.r4.model.Patient
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * Selects at most one patient from the set of resources being
 * sent to the CHDP. Returns the first of each category:
 *
 * - Tagged Update
 * - Tagged Create
 * - Untagged Update
 * - Untagged Create
 */
@Component
class PatientDeduplicatorUseCase(
    @Value("\${patient-deduplicator.chdp-system}")
    private val chdpSystem: String,
    @Value("\${patient-deduplicator.chdp-code}")
    private val chdpCode: String,
) {
    operator fun invoke(transaction: Transaction): Transaction {
        val (patientsToUpdate, restToUpdate) = transaction
            .toUpdate
            .partition { it is Patient }
            .let { (patients, restToUpdate) ->
                patients.filterIsInstance<Patient>() to restToUpdate
            }

        val (patientsToCreate, restToCreate) = transaction
            .toCreate
            .partition { it is Patient }
            .let { (patients, restToCreate) ->
                patients.filterIsInstance<Patient>() to restToCreate
            }

        val patientsToUpdateWithKind = patientsToUpdate
            .map { patient ->
                val tag = if (patient.hasChdpTag()) {
                    PatientKind.TaggedUpdate
                } else {
                    PatientKind.UntaggedUpdate
                }

                tag to patient
            }

        val patientsToCreateWithKind = patientsToCreate
            .map { patient ->
                val tag = if (patient.hasChdpTag()) {
                    PatientKind.TaggedCreate
                } else {
                    PatientKind.UntaggedCreate
                }

                tag to patient
            }

        val candidate = patientsToUpdateWithKind
            .plus(patientsToCreateWithKind)
            .minByOrNull { it.first.priority }

        return when (candidate?.first) {
            PatientKind.TaggedUpdate, PatientKind.UntaggedUpdate -> Transaction(
                toUpdate = restToUpdate.plus(candidate.second),
                toCreate = restToCreate,
            )

            PatientKind.TaggedCreate, PatientKind.UntaggedCreate -> Transaction(
                toUpdate = restToUpdate,
                toCreate = restToCreate.plus(candidate.second),
            )

            null -> Transaction(
                toUpdate = restToUpdate,
                toCreate = restToCreate,
            )
        }
    }

    private fun Patient.hasChdpTag() = meta.tag.any { coding ->
        coding.system == chdpSystem && coding.code == chdpCode
    }
}

private enum class PatientKind(
    val priority: Int,
) {
    TaggedUpdate(100),
    TaggedCreate(200),
    UntaggedUpdate(300),
    UntaggedCreate(400),
}
