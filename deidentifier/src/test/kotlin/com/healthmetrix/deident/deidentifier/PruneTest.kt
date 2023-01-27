package com.healthmetrix.deident.deidentifier

import com.healthmetrix.deident.commons.test.FhirHelper
import org.assertj.core.api.Assertions.assertThat
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.StringType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PruneTest {
    private val fhirHelper = FhirHelper()

    private lateinit var questionnaireResponse: QuestionnaireResponse

    @BeforeEach
    fun beforeEach() {
        questionnaireResponse = fhirHelper.load("QuestionnaireResponse.json")
    }

    @Test
    fun `pruning a resource with no live elements results in a resource with no child values`() {
        questionnaireResponse.prune(setOf())

        assertThat(fhirHelper.path(questionnaireResponse, "descendants()")).isEmpty()
    }

    @Test
    fun `pruning a resource with one live child results in a resource with one live child`() {
        val liveChild = fhirHelper.path(questionnaireResponse, "QuestionnaireResponse.subject").single()

        questionnaireResponse.prune(setOf(liveChild))

        val descendants = fhirHelper.path(questionnaireResponse, "descendants()")
        assertThat(descendants).hasSize(1)
        assertThat(descendants.single()).isInstanceOf(Reference::class.java)
    }

    @Test
    fun `pruning with a living nested child and keepParentsAlive true keeps the family tree alive`() {
        val liveChild = fhirHelper.path(questionnaireResponse, "QuestionnaireResponse.subject.reference").single()

        questionnaireResponse.prune(setOf(liveChild), keepParentsAlive = true)

        val descendants = fhirHelper.path(questionnaireResponse, "descendants()")
        assertThat(descendants).hasSize(2)
        assertThat(descendants.singleOrNull { it is Reference }).isNotNull
        assertThat(descendants.singleOrNull { it is StringType }).isNotNull
    }

    @Test
    fun `pruning with a living nested child and keepParentsAlive false prunes the whole tree`() {
        val liveChild = fhirHelper.path(questionnaireResponse, "QuestionnaireResponse.subject.reference").single()

        questionnaireResponse.prune(setOf(liveChild), keepParentsAlive = false)

        assertThat(fhirHelper.path(questionnaireResponse, "descendants()")).isEmpty()
    }
}
