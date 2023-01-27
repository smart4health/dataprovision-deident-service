package com.healthmetrix.deident.contextualizer

import com.healthmetrix.deident.commons.test.FhirHelper
import org.assertj.core.api.Assertions.assertThat
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.junit.jupiter.api.Test

class ContextualizeUseCaseTest {

    private val d4lId = "newD4lId"

    private val system = "system"

    private val fhirHelper = FhirHelper()

    private val underTest = ContextualizeUseCase(system)

    @Test
    fun `contextualizing questionnaire response without subject adds d4l id`() {
        val questionnaireResponse =
            fhirHelper.load<QuestionnaireResponse>("QuestionnaireResponse_no_subject.json")

        underTest(questionnaireResponse, d4lId)

        assertThat(questionnaireResponse.subject.identifier.value).isEqualTo(d4lId)
        assertThat(questionnaireResponse.subject.identifier.system).isEqualTo(system)
        assertThat(questionnaireResponse.subject.hasReference()).isFalse
    }

    @Test
    fun `contextualizing questionnaire response with subject reference does not add patientId`() {
        val questionnaireResponse =
            fhirHelper.load<QuestionnaireResponse>("QuestionnaireResponse_subject_reference.json")

        underTest(questionnaireResponse, d4lId)

        assertThat(questionnaireResponse.subject.reference).isEqualTo("originalReference")
        assertThat(questionnaireResponse.subject.hasIdentifier()).isFalse
    }

    @Test
    fun `contextualizing questionnaire response with subject identifier does not add patientId`() {
        val questionnaireResponse =
            fhirHelper.load<QuestionnaireResponse>("QuestionnaireResponse_subject_identifier.json")

        underTest(questionnaireResponse, d4lId)

        assertThat(questionnaireResponse.subject.hasReference()).isFalse
        assertThat(questionnaireResponse.subject.identifier.value).isEqualTo("originalReference")
    }
}
