package com.healthmetrix.deident.deidentifier.jsonfilter.method

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.unwrap
import com.healthmetrix.deident.commons.replace
import com.healthmetrix.deident.commons.test.FhirHelper
import org.assertj.core.api.Assertions.assertThat
import org.hl7.fhir.r4.model.Questionnaire
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ReplaceMethodTest {
    private val fhirHelper = FhirHelper()

    private lateinit var questionnaire: Questionnaire

    private val underTest = ReplaceMethod(ReplaceMethod.Params(with = "<REPLACE SUCCESSFUL>"))

    @BeforeEach
    fun beforeEach() {
        questionnaire = fhirHelper.load("Questionnaire.json")
    }

    @Test
    fun `replace method replaces the targeted string with the params-with text`() {
        val before = questionnaire.let(fhirHelper::json)

        fhirHelper.path(questionnaire, "Questionnaire.item[0].text").forEach {
            val transformed = underTest.accept(it).unwrap()
            assertThat(transformed !== it)
            val res = questionnaire.replace(it, transformed)
            assertThat(res).isInstanceOf(Ok::class.java)
        }

        val after = questionnaire.let(fhirHelper::json)

        assertThat(before).isNotEqualTo(after)
        assertThat(after).containsSequence("<REPLACE SUCCESSFUL>")
    }
}
