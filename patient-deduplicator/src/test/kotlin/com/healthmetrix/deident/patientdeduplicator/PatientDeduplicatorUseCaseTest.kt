package com.healthmetrix.deident.patientdeduplicator

import com.healthmetrix.deident.commons.Transaction
import org.assertj.core.api.Assertions.assertThat
import org.hl7.fhir.r4.model.AllergyIntolerance
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.junit.jupiter.api.Test

class PatientDeduplicatorUseCaseTest {

    private val underTest = PatientDeduplicatorUseCase(
        chdpSystem = "example-system",
        chdpCode = "example-code",
    )

    // since equality is referential for these...
    val fakeObservation = Observation()
    val fakeCondition = Condition()
    val fakeQuestionnaire = Questionnaire()
    val fakeAllergyIntolerance = AllergyIntolerance()
    val fakeUntaggedPatient = Patient()
    val fakeWrongTaggedPatient = Patient().apply {
        meta.tag = listOf(
            Coding("wrong-system", "wrong-code", "display!"),
        )
    }
    val fakeTaggedPatient = Patient().apply {
        meta.tag = listOf(
            Coding("example-system", "example-code", "display!"),
        )
    }

    @Test
    fun `transactions with no patients are not changed`() {
        val transaction = Transaction(
            toUpdate = listOf(fakeObservation, fakeCondition),
            toCreate = listOf(fakeQuestionnaire, fakeAllergyIntolerance),
        )

        val actual = underTest(transaction)

        assertThat(actual).isEqualTo(transaction)
    }

    @Test
    fun `transactions with untagged patients select the first to update`() {
        val transaction = Transaction(
            toUpdate = listOf(fakeObservation, fakeUntaggedPatient, fakeCondition),
            toCreate = listOf(fakeQuestionnaire, fakeUntaggedPatient, fakeAllergyIntolerance),
        )

        val actual = underTest(transaction)

        val expected = Transaction(
            toUpdate = listOf(fakeObservation, fakeCondition, fakeUntaggedPatient),
            toCreate = listOf(fakeQuestionnaire, fakeAllergyIntolerance),
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `transactions with wrong tagged patients select the first to update`() {
        val transaction = Transaction(
            toUpdate = listOf(fakeObservation, fakeWrongTaggedPatient, fakeCondition),
            toCreate = listOf(fakeQuestionnaire, fakeWrongTaggedPatient, fakeAllergyIntolerance),
        )

        val actual = underTest(transaction)

        val expected = Transaction(
            toUpdate = listOf(fakeObservation, fakeCondition, fakeWrongTaggedPatient),
            toCreate = listOf(fakeQuestionnaire, fakeAllergyIntolerance),
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `transactions with one correctly tagged patient is not filtered`() {
        val transaction = Transaction(
            toUpdate = listOf(fakeObservation, fakeTaggedPatient, fakeCondition),
            toCreate = listOf(fakeQuestionnaire, fakeAllergyIntolerance),
        )

        val actual = underTest(transaction)

        // There is some reordering though
        val expected = Transaction(
            toUpdate = listOf(fakeObservation, fakeCondition, fakeTaggedPatient),
            toCreate = listOf(fakeQuestionnaire, fakeAllergyIntolerance),
        )

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `transactions with multiple tagged patients select the one in toUpdate`() {
        val transaction = Transaction(
            toUpdate = listOf(fakeObservation, fakeTaggedPatient, fakeCondition),
            toCreate = listOf(fakeQuestionnaire, fakeTaggedPatient, fakeAllergyIntolerance),
        )

        val actual = underTest(transaction)

        val expected = Transaction(
            toUpdate = listOf(fakeObservation, fakeCondition, fakeTaggedPatient),
            toCreate = listOf(fakeQuestionnaire, fakeAllergyIntolerance),
        )
        assertThat(actual).isEqualTo(expected)
    }
}
