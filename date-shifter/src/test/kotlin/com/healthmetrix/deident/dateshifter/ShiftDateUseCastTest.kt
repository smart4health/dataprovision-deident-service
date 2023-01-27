package com.healthmetrix.deident.dateshifter

import com.healthmetrix.deident.commons.test.FhirHelper
import org.assertj.core.api.Assertions.assertThat
import org.hl7.fhir.r4.model.Patient
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Date

class ShiftDateUseCastTest {

    private val fhirHelper = FhirHelper()

    private val testZoneId = ZoneId.of("CET")

    private val t0 = LocalDate.of(2021, 3, 15).atStartOfDay(testZoneId)

    private val patient = {
        Patient().apply {
            birthDate = t0.toInstant().let(Date::from)
        }
    }

    private val oldPatient = {
        Patient().apply {
            birthDate = t0.minusDays(14).toInstant().let(Date::from)
        }
    }

    private val underTest = ShiftDateUseCase(
        paths = listOf("Patient.birthDate"),
        fhirPath = fhirHelper.fhirPath,
        cutoffStart = t0.minusDays(7),
        cutoffEnd = { t0.plusDays(7) },
    )

    @ParameterizedTest
    @ValueSource(ints = [1, 2, 3, 4, 5, 6])
    fun `shifting date forwards within cutoff results in keep outcome`(shiftDays: Int) {
        val patient = patient()
        val outcome = underTest(patient, shiftDays)

        assertThat(patient.birthDate.toZonedDateTime()).isEqualTo(t0.plusDays(shiftDays.toLong()))
        assertThat(outcome).isEqualTo(ShiftDateUseCase.Outcome.KEEP)
    }

    @Test
    fun `shifting date to the day of the cutoff results in keep outcome`() {
        val patient = patient()
        val outcome = underTest(patient, 7)

        assertThat(patient.birthDate.toZonedDateTime()).isEqualTo(t0.plusDays(7))
        assertThat(outcome).isEqualTo(ShiftDateUseCase.Outcome.KEEP)
    }

    @ParameterizedTest
    @ValueSource(ints = [8, 9, 10, 355])
    fun `shifting date beyond cutoff results in truncate end outcome`(shiftDays: Int) {
        val patient = patient()
        val outcome = underTest(patient, shiftDays)

        assertThat(patient.birthDate.toZonedDateTime()).isEqualTo(t0.plusDays(shiftDays.toLong()))
        assertThat(outcome).isEqualTo(ShiftDateUseCase.Outcome.TRUNCATE_END)
    }

    @ParameterizedTest
    @ValueSource(ints = [8, 9, 10])
    fun `shifting old dates forwards within cutoff results in keep outcome`(shiftDays: Int) {
        val patient = oldPatient()
        val outcome = underTest(patient, shiftDays)

        assertThat(patient.birthDate.toZonedDateTime()).isEqualTo(t0.plusDays(shiftDays.toLong() - 14))
        assertThat(outcome).isEqualTo(ShiftDateUseCase.Outcome.KEEP)
    }

    @Test
    fun `shifting old dates to the day of the cutoff start results in keep outcome`() {
        val patient = oldPatient()
        val outcome = underTest(patient, 7)

        assertThat(patient.birthDate.toZonedDateTime()).isEqualTo(t0.minusDays(7))
        assertThat(outcome).isEqualTo(ShiftDateUseCase.Outcome.KEEP)
    }

    @ParameterizedTest
    @ValueSource(ints = [1, 2, 3, 4, 5, 6])
    fun `shifting old dates forwards but before cutoff results in truncate start outcome`(shiftDays: Int) {
        val patient = oldPatient()
        val outcome = underTest(patient, shiftDays)

        assertThat(patient.birthDate.toZonedDateTime()).isEqualTo(t0.plusDays(shiftDays.toLong() - 14))
        assertThat(outcome).isEqualTo(ShiftDateUseCase.Outcome.TRUNCATE_START)
    }

    private fun Date.toZonedDateTime(): ZonedDateTime =
        ZonedDateTime.ofInstant(toInstant(), testZoneId)
}
