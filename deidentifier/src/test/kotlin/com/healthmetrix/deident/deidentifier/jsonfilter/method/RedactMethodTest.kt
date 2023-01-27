package com.healthmetrix.deident.deidentifier.jsonfilter.method

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.unwrap
import com.healthmetrix.deident.commons.replace
import com.healthmetrix.deident.commons.test.FhirHelper
import org.assertj.core.api.Assertions.assertThat
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RedactMethodTest {
    private val fhirHelper = FhirHelper()

    private lateinit var questionnaireResponse: QuestionnaireResponse

    private val underTest = RedactMethod()

    @BeforeEach
    fun beforeEach() {
        questionnaireResponse = fhirHelper.load("QuestionnaireResponse.json")
    }

    @Test
    fun `redact method removes the targeted field`() {
        val before = questionnaireResponse.let(fhirHelper::json)

        fhirHelper.path(questionnaireResponse, "QuestionnaireResponse.subject").forEach {
            val transformed = underTest.accept(it).unwrap()
            assertThat(transformed).isNull()
            val res = questionnaireResponse.replace(it, transformed)
            assertThat(res).isInstanceOf(Ok::class.java)
        }

        val after = questionnaireResponse.let(fhirHelper::json)

        assertThat(before).isNotEqualTo(after)
        assertThat(after).doesNotContain("subject")
    }

    @Test
    fun `redact method removes a non top level field`() {
        val before = questionnaireResponse.let(fhirHelper::json)

        fhirHelper.path(questionnaireResponse, "QuestionnaireResponse.item.answer").forEach {
            val transformed = underTest.accept(it).unwrap()
            assertThat(transformed).isNull()
            val res = questionnaireResponse.replace(it, transformed)
            assertThat(res).isInstanceOf(Ok::class.java)
        }

        val after = questionnaireResponse.let(fhirHelper::json)

        assertThat(before).isNotEqualTo(after)
        assertThat(after).doesNotContain("answer")
    }
}
