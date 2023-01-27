package com.healthmetrix.deident.deidentifier.jsonfilter.method

import com.github.michaelbull.result.unwrap
import com.healthmetrix.deident.commons.test.FhirHelper
import org.assertj.core.api.Assertions.assertThat
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class KeepMethodTest {

    private val fhirHelper = FhirHelper()

    private lateinit var questionnaireResponse: QuestionnaireResponse

    private val underTest = KeepMethod()

    @BeforeEach
    fun beforeEach() {
        questionnaireResponse = fhirHelper.load("QuestionnaireResponse.json")
    }

    @Test
    fun `keep method does not change the object`() {
        val before = questionnaireResponse.let(fhirHelper::json)

        fhirHelper.path(questionnaireResponse, "QuestionnaireResponse.subject").forEach {
            val transformed = underTest.accept(it).unwrap()
            assertThat(transformed).isEqualTo(it)
        }

        val after = questionnaireResponse.let(fhirHelper::json)

        assertThat(before).isEqualTo(after)
    }
}
