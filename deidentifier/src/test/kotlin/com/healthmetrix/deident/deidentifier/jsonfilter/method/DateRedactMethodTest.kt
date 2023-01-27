package com.healthmetrix.deident.deidentifier.jsonfilter.method

import com.github.michaelbull.result.unwrap
import com.healthmetrix.deident.commons.replace
import org.assertj.core.api.Assertions.assertThat
import org.hl7.fhir.r4.model.DateType
import org.hl7.fhir.r4.model.Patient
import org.junit.jupiter.api.Test
import java.time.Year

class DateRedactMethodTest {

    private val baseYear = 2021

    private val underTest = DateRedactMethod { Year.of(baseYear) }

    @Test
    fun `dateRedacting a 21 year old truncates to year`() {
        val patient = Patient().apply {
            birthDateElement = DateType(baseYear - 21, 4, 10)
        }

        patient.replace(patient.birthDateElement, underTest.accept(patient.birthDateElement).unwrap())

        assertThat(patient.birthDateElement.toHumanDisplay()).isEqualTo("2000")
    }

    @Test
    fun `dateRedacting an 89 year old truncates to year`() {
        val patient = Patient().apply {
            birthDateElement = DateType(baseYear - 89, 4, 10)
        }

        patient.replace(patient.birthDateElement, underTest.accept(patient.birthDateElement).unwrap())

        assertThat(patient.birthDateElement.toHumanDisplay()).isEqualTo("1932")
    }

    @Test
    fun `dateRedacting a 100 year old redacts fully`() {
        val patient = Patient().apply {
            birthDateElement = DateType(baseYear - 100, 4, 10)
        }

        patient.replace(patient.birthDateElement, underTest.accept(patient.birthDateElement).unwrap())

        // birthDateElement auto creates
        assertThat(patient.birthDate).isNull()
    }

    @Test
    fun `dateRedacting a 90 year old redacts fully`() {
        val patient = Patient().apply {
            birthDateElement = DateType(baseYear - 90, 4, 10)
        }

        patient.replace(patient.birthDateElement, underTest.accept(patient.birthDateElement).unwrap())

        // birthDateElement auto creates
        assertThat(patient.birthDate).isNull()
    }
}
