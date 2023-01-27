package com.healthmetrix.deident.harmonizer.harmonize

import ca.uhn.fhir.fhirpath.IFhirPath
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.healthmetrix.deident.harmonizer.qomopclient.QomopClient
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Resource
import org.springframework.stereotype.Component
import com.github.michaelbull.result.runCatching as catch

@Component
class ResourceHarmonizeUseCase(
    private val fhirPath: IFhirPath,
    private val qomopClient: QomopClient,
) {
    /**
     * Harmonize given Resource for Harmonization:
     * 1. Look up all descendants of type CodeableConcept starting from the root resource (recursive) via. FhirPath
     * 2. Skip CodeableConcepts which have invalid Coding items (either system or code null)
     * 3. Harmonize given Coding items and replace in CodeableConcept resource
     *
     * FhirPath expressions, see
     * - [org.hl7.fhir.r4.model.ExpressionNode.Function.Descendants]
     * - [org.hl7.fhir.r4.model.ExpressionNode.Function.OfType]
     *
     * @param resource FHIR resource
     * @return
     */
    operator fun invoke(resource: Resource): Result<Unit, Throwable> = catch {
        fhirPath.evaluate(resource, "descendants().ofType(CodeableConcept)", CodeableConcept::class.java)
            .forEach { codeableConcept ->
                codeableConcept.coding = codeableConcept.coding
                    .map { it to qomopClient.harmonize(it) }
                    .map { (original, harmonizationResponse) ->
                        when (harmonizationResponse) {
                            is Ok -> harmonizationResponse.value.toNewCoding(original)
                            is Err -> original
                        }
                    }
            }
    }
}
