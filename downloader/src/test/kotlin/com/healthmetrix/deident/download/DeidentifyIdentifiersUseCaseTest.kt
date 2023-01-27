package com.healthmetrix.deident.download

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.michaelbull.result.Ok
import com.healthmetrix.deident.commons.test.FhirHelper
import org.assertj.core.api.Assertions.assertThat
import org.hl7.fhir.r4.model.Bundle
import org.junit.jupiter.api.Test

class DeidentifyIdentifiersUseCaseTest {

    private val fhirHelper = FhirHelper()

    private val objectMapper = ObjectMapper()

    private val underTest = DeidentifyIdentifiersUseCase(
        fhirPath = fhirHelper.fhirContext.newFhirPath(),
    )

    private val fakeSecret = ByteArray(32) { 0 }

    @Test
    fun `deidentifying different style references to the same patient works`() {
        val input = fhirHelper.load<Bundle>("input.bundle.json")
        val output = fhirHelper.load<Bundle>("output.bundle.json")

        val result = underTest(input, fakeSecret)

        assertThat(result is Ok)

        val actualJsonNode = input
            .let(fhirHelper::json)
            .let(objectMapper::readTree)

        val expectedJsonNode = output
            .let(fhirHelper::json)
            .let(objectMapper::readTree)

        assertThat(actualJsonNode).isEqualTo(expectedJsonNode)
    }
}
