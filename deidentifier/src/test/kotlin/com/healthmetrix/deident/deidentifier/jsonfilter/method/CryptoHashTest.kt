package com.healthmetrix.deident.deidentifier.jsonfilter.method

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.unwrap
import com.healthmetrix.deident.commons.replace
import com.healthmetrix.deident.commons.test.FhirHelper
import org.assertj.core.api.Assertions.assertThat
import org.hl7.fhir.instance.model.api.IBase
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.IdType
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Reference
import org.junit.jupiter.api.Test

class CryptoHashTest {

    private val fhirHelper = FhirHelper()

    private val underTest = CryptoHashMethod()

    @Test
    fun `cryptohashing an id does nothing`() {
        val patient = fhirHelper.load<Patient>()
        val before = patient.let(fhirHelper::json)

        val id = fhirHelper.path(patient, "Patient.id").single()

        patient.replace(id, underTest.accept(id).unwrap())

        val after = patient.let(fhirHelper::json)

        assertThat(before).isEqualTo(after)
    }

    @Test
    fun `cryptohashing a reference does nothing`() {
        val questionnaireResponse = fhirHelper.load<QuestionnaireResponse>()

        val before = questionnaireResponse.let(fhirHelper::json)

        val subject = fhirHelper.path(questionnaireResponse, "QuestionnaireResponse.subject").single()

        questionnaireResponse.replace(subject, underTest.accept(subject).unwrap())

        val after = questionnaireResponse.let(fhirHelper::json)

        assertThat(before).isEqualTo(after)
    }

    @Test
    fun `cryptohashing an identifier does nothing`() {
        val encounter = fhirHelper.load<Encounter>()

        val before = encounter.let(fhirHelper::json)

        val identifier = fhirHelper.path(encounter, "Encounter.identifier[0]").single()

        encounter.replace(identifier, underTest.accept(identifier).unwrap())

        val after = encounter.let(fhirHelper::json)

        assertThat(before).isEqualTo(after)
    }

    @Test
    fun `cryptohash only accepts references, ids, identifiers`() {
        val encounter = fhirHelper.load<Encounter>()

        fhirHelper.path(encounter, "Encounter.descendants()")
            .map(underTest::accept)
            .filterIsInstance<Ok<IBase?>>()
            .map(Ok<IBase?>::unwrap)
            .all { it is Reference || it is IdType || it is Identifier }
            .let { assertThat(it).isTrue }
    }
}
