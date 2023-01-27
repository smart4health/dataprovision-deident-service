package com.healthmetrix.deident.harmonizer.harmonize

import ca.uhn.fhir.context.FhirContext
import com.healthmetrix.deident.commons.test.FhirHelper
import org.assertj.core.api.Assertions.assertThat
import org.hl7.fhir.r4.model.Basic
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

/**
 * Acceptance test for the harmonization process. Only uses the In-Memory databases so consider adding
 * items to add more sophisticated harmonization cases.
 */
@Suppress("FunctionName")
@SpringBootTest(properties = ["springdoc.swagger-ui.enabled=false", "spring.cloud.vault.enabled=false"])
class ResourceHarmonizeUseCaseAcceptanceTest {

    private lateinit var basicResource: Basic

    @Autowired
    private lateinit var fhirContext: FhirContext

    private val fhirHelper by lazy { FhirHelper(fhirContext) }

    @Autowired
    private lateinit var resourceHarmonizeUseCase: ResourceHarmonizeUseCase

    @BeforeEach
    fun beforeEach() {
        basicResource = fhirHelper.load("fhir/harmonization_input.json")
    }

    @Test
    fun `convert icd codings and compare with mocked result JSON`() {
        val expected = javaClass.classLoader
            .getResource("fhir/harmonization_expected_no_qomop.json")!!
            .readText()

        resourceHarmonizeUseCase(basicResource)

        val jsonResult = fhirHelper.json(basicResource)
        assertThat(jsonResult).isEqualTo(expected)
    }
}
